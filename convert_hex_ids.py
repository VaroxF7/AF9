#!/usr/bin/env python3
import json, re, copy

with open('config/ftbquests/quests/chapters/circuits_only.snbt', 'r', encoding='utf-8') as f:
    content = f.read()

# Parse the SNBT as JSON (it's valid JSON format)
data = json.loads(content)

# Create a mapping from old IDs to new 16-hex-digit IDs
# We'll use sequential hex IDs: 0000000000000001, 0000000000000002, etc.
id_mapping = {}
hex_counter = 1

def to_hex16(s):
    global hex_counter
    # Generate a 16-char hex ID
    hex_str = format(hex_counter, '016X')  # 16 uppercase hex digits
    id_mapping[s] = hex_str
    hex_counter += 1
    return hex_str

# Walk through the data and replace all 'id' fields
def walk_and_replace(obj):
    global hex_counter
    if isinstance(obj, dict):
        # Replace 'id' field if present and not colon-containing
        if 'id' in obj and isinstance(obj['id'], str) and ':' not in obj['id']:
            old_id = obj['id']
            if old_id not in id_mapping:
                id_mapping[old_id] = to_hex16(old_id)
            obj['id'] = id_mapping[old_id]
        
        # Recursively process all values
        for key in obj:
            obj[key] = walk_and_replace(obj[key])
    elif isinstance(obj, list):
        for i, item in enumerate(obj):
            obj[i] = walk_and_replace(item)
    return obj

# Replace all IDs
new_data = walk_and_replace(copy.deepcopy(data))

# Now we need to update all references to the old IDs
# This includes: dependencies, linked_quest, task item references, reward references

# First, let me collect all the old IDs that were used as references
# Then update all references to use the new hex IDs

# Actually, the walk_and_replace already updated the 'id' fields.
# Now I need to update references like dependencies[] linked_quest, etc.

# Let me find all the old IDs that were used as references
old_ids_used_as_refs = set()

def collect_refs(obj):
    if isinstance(obj, dict):
        # Check for dependencies
        if 'dependencies' in obj and isinstance(obj['dependencies'], list):
            for dep in obj['dependencies']:
                if dep in id_mapping:
                    old_ids_used_as_refs.add(dep)
        # Check for linked_quest
        if 'linked_quest' in obj and isinstance(obj['linked_quest'], str):
            if obj['linked_quest'] in id_mapping:
                old_ids_used_as_refs.add(obj['linked_quest'])
        # Check task item references
        if 'tasks' in obj and isinstance(obj['tasks'], list):
            for task in obj['tasks']:
                if 'item' in task and isinstance(task['item'], str):
                    if task['item'] in id_mapping:
                        old_ids_used_as_refs.add(task['item'])
                if 'id' in task and isinstance(task['id'], str) and ':' not in task['id']:
                    if task['id'] in id_mapping:
                        old_ids_used_as_refs.add(task['id'])
        # Check reward references
        if 'rewards' in obj and isinstance(obj['rewards'], list):
            for reward in obj['rewards']:
                if 'id' in reward and isinstance(reward['id'], str) and ':' not in reward['id']:
                    if reward['id'] in id_mapping:
                        old_ids_used_as_refs.add(reward['id'])
        # Recurse
        for key in obj:
            collect_refs(obj[key])
    return old_ids_used_as_refs

# Collect all old IDs used as references
collect_refs(new_data)

print("Old IDs mapped:")
for old, new in id_mapping.items():
    print(f"  {old} -> {new}")

print("\nOld IDs used as references:")
for ref in old_ids_used_as_refs:
    print(f"  Reference: {ref}")

# Now update all references in the new data to use new hex IDs
def update_refs(obj, id_mapping):
    if isinstance(obj, dict):
        # Update dependencies
        if 'dependencies' in obj and isinstance(obj['dependencies'], list):
            obj['dependencies'] = [id_mapping.get(dep, dep) for dep in obj['dependencies']]
        
        # Update linked_quest
        if 'linked_quest' in obj and isinstance(obj['linked_quest'], str):
            obj['linked_quest'] = id_mapping.get(obj['linked_quest'], obj['linked_quest'])
        
        # Update task item references
        if 'tasks' in obj and isinstance(obj['tasks'], list):
            for task in obj['tasks']:
                if 'item' in task and isinstance(task['item'], str):
                    task['item'] = id_mapping.get(task['item'], task['item'])
                if 'id' in task and isinstance(task['id'], str) and ':' not in task['id']:
                    task['id'] = id_mapping.get(task['id'], task['id'])
        
        # Update reward references
        if 'rewards' in obj and isinstance(obj['rewards'], list):
            for reward in obj['rewards']:
                if 'id' in reward and isinstance(reward['id'], str) and ':' not in reward['id']:
                    reward['id'] = id_mapping.get(reward['id'], reward['id'])
        
        # Recurse
        for key in obj:
            obj[key] = update_refs(obj[key], id_mapping)
    return obj

# Update references
new_data = update_refs(new_data, id_mapping)

# Convert back to string
# We need to preserve the SNBT format. Let me just use json.dumps with some formatting.
# Actually, the original file uses a specific format. Let me just output as JSON with proper formatting.

# Convert to JSON string
# Need to make sure the IDs are preserved as strings
output = json.dumps(new_data, indent=4, ensure_ascii=False)

# The original file had some formatting differences (no quotes on some things, etc.)
# Let me just try the JSON output first and see if the lint accepts it

with open('config/ftbquests/quests/chapters/circuits_only.snbt', 'w', encoding='utf-8') as f:
    f.write(output)
print("\nFile written with hex IDs")
print(f"Total IDs mapped: {len(id_mapping)}")