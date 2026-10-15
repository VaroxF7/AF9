// AF9 - Alchemistry dissolver recipes whose input matches no item. Alchemistry builds an IngredientStack on the client
// with ingredient.getItems()[0], so one dissolver recipe with an empty input (a tag that nothing is in, once GT and AF9
// have changed the tags) throws ArrayIndexOutOfBoundsException while the client decodes the recipe packet. A player on
// the host's own world never decodes that packet; everyone who joins over LAN/Essential/a server is dropped right after
// login ("Internal Exception: java.lang.ArrayIndexOutOfBoundsException: Index 0 out of bounds for length 0"). The
// recipes are removed here and listed in logs/kubejs/server.log.

ServerEvents.recipes(event => {
    let empty = []
    // (batched: Ingredient.of resolved once per spec string, not once per recipe)
    let ingredientCache = {}

    event.forEachRecipe({ type: 'alchemistry:dissolver' }, recipe => {
        let input = recipe.json.get('input')
        let ingredient = input && input.isJsonObject() ? input.getAsJsonObject().get('ingredient') : null
        if (!ingredient || !ingredient.isJsonObject()) return
        let obj = ingredient.getAsJsonObject()

        let spec = null
        if (obj.has('tag')) spec = '#' + obj.get('tag').getAsString()
        else if (obj.has('item')) spec = obj.get('item').getAsString()
        if (spec === null) return

        if (ingredientCache[spec] === undefined) ingredientCache[spec] = Ingredient.of(spec)
        if (ingredientCache[spec].getFirst().isEmpty()) empty.push({ id: String(recipe.getId()), spec: spec })
    })

    empty.forEach(r => {
        console.warn('AF9: dissolver recipe ' + r.id + ' has an empty input (' + r.spec + '), removed')
    })
    // (batched: the same collected ids removed in one filter call)
    empty.forEach(r => event.remove({ id: r.id }))
    console.info('AF9: removed ' + empty.length + ' alchemistry dissolver recipes with an empty input')
})
