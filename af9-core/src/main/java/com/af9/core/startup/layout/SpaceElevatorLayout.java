package com.af9.core.startup.layout;

/**
 * The Space Elevator's structure as slices of characters (no GregTech in here), GTNH's tower block for block and its
 * extension ring. {@link #slices(boolean)} gives what the pattern is built from: aisles front -> back (a slice each),
 * rows top -> bottom, as the tables are written.
 */
public final class SpaceElevatorLayout {

    private SpaceElevatorLayout() {}

    // GTNH's Space Elevator (gtnhintergalactic's TileEntitySpaceElevator, STRUCTURE_PIECE_MAIN; design: Sampsa, Jimbno, Adam,
    // Baunti), block for block: 35 wide, 43 high, 35 deep. The table holds the slices from the front to the middle one (the
    // back half mirrors the front), each slice its rows from its highest block down to the floor (the tower narrows), a row
    // its blocks from left to right:
    //   A  Ultra High Strength Concrete Floor   D  Base Casing            E  Stress-Proof Casing       F  Internal Structure
    //   H  Neutronium Frame Box                 C  Motor (one tier)       B  the Cable             -  air (the shaft)
    //   X  Base Casing or a hatch of the elevator: the bottom centre casings (and the controller, in the front one's middle)
    //   M  Base Casing or a bus / hatch of a module slot                  I  a module slot: a Mining Module, or Base Casing
    // Hatches: energy and laser in X; input bus, Coolant Hatch, fluid output hatch, fluid input hatch and output bus in X or M.
    private static final String[][] SE_MAIN = {
    {   // 0
        "               FF FF               ",
        "               AAAAA               "
    },
    {   // 1
        "               D   D               ",
        "            FFFFF FFFFF            ",
        "            AAAAAAAAAAA            "
    },
    {   // 2
        "            DDDDE EDDDD            ",
        "          FFFFFFF FFFFFFF          ",
        "          AAAAAAAAAAAAAAA          "
    },
    {   // 3
        "                E E                ",
        "          DD   DE ED   DD          ",
        "         FFFFFFD   DFFFFFF         ",
        "        AAAAAAAAAAAAAAAAAAA        "
    },
    {   // 4
        "                E E                ",
        "               DE ED               ",
        "               D   D               ",
        "         FFF           FFF         ",
        "       AAAAAAAAAAAAAAAAAAAAA       "
    },
    {   // 5
        "                E E                ",
        "               DE ED               ",
        "               DE ED               ",
        "                                   ",
        "                                   ",
        "      AAAAAAAAAAAAAAAAAAAAAAA      "
    },
    {   // 6
        "                E E                ",
        "               DE ED               ",
        "               DE ED               ",
        "               DE ED               ",
        "                                   ",
        "                                   ",
        "                                   ",
        "     AAAAAAAAAAAAAAAAAAAAAAAAA     "
    },
    {   // 7
        "                E E                ",
        "              HDE EDH              ",
        "               DE ED               ",
        "               DE ED               ",
        "               DE ED               ",
        "                                   ",
        "                                   ",
        "                                   ",
        "                                   ",
        "                                   ",
        "    AAAAAAAAAAAAAAAAAAAAAAAAAAA    "
    },
    {   // 8
        "                E E                ",
        "               DE ED               ",
        "               DE ED               ",
        "               DE ED               ",
        "            HH DE ED HH            ",
        "                                   ",
        "                                   ",
        "                                   ",
        "                                   ",
        "               M M M               ",
        "               M M M               ",
        "               M M M               ",
        "               M M M               ",
        "   AAAAAAAAAAAAM M MAAAAAAAAAAAA   "
    },
    {   // 9
        "                E E                ",
        "               DE ED               ",
        "               DE ED               ",
        "               DE ED               ",
        "               DE ED               ",
        "                                   ",
        "         E               E         ",
        "         EHH           HHE         ",
        "         E               E         ",
        "         E               E         ",
        "         E               E         ",
        "         E               E         ",
        "         E     M M M     E         ",
        "         E     I I I     E         ",
        "         E     M M M     E         ",
        "   FF    E     M M M     E    FF   ",
        "   AAAAAAAAAAAAM M MAAAAAAAAAAAA   "
    },
    {   // 10
        "                E E                ",
        "               DE ED               ",
        "               DE ED               ",
        "               DE ED               ",
        "               DE ED               ",
        "               DE ED               ",
        "          E             E          ",
        "          E             E          ",
        "          E             E          ",
        "          E             E          ",
        "          E             E          ",
        "         HE             EH         ",
        "                                   ",
        "                                   ",
        "                                   ",
        "                                   ",
        "                                   ",
        "                                   ",
        "   D                           D   ",
        "  FFF          F F F          FFF  ",
        "  AAAAAAAAAAAAA     AAAAAAAAAAAAA  "
    },
    {   // 11
        "                E E                ",
        "               DE ED               ",
        "               DE ED               ",
        "               DE ED               ",
        "               DE ED               ",
        "               DE ED               ",
        "               DE ED               ",
        "               DE ED               ",
        "               DE ED               ",
        "               DE ED               ",
        "                                   ",
        "                                   ",
        "           E           E           ",
        "           E           E           ",
        "           E           E           ",
        "           E           E           ",
        "           E           E           ",
        "                                   ",
        "                                   ",
        "         H               H         ",
        "                                   ",
        "                                   ",
        "                                   ",
        "                                   ",
        "                                   ",
        "                                   ",
        "   D                           D   ",
        "  FFF          F F F          FFF  ",
        "  AAAAAAAAAAAAA     AAAAAAAAAAAAA  "
    },
    {   // 12
        "                E E                ",
        "               DE ED               ",
        "               DE ED               ",
        "               DE ED               ",
        "               DE ED               ",
        "               DE ED               ",
        "               DE ED               ",
        "               DE ED               ",
        "               DE ED               ",
        "                                   ",
        "                                   ",
        "                                   ",
        "                                   ",
        "                                   ",
        "             HH     HH             ",
        "            E         E            ",
        "            E         E            ",
        "            E         E            ",
        "            E         E            ",
        "            E         E            ",
        "            E         E            ",
        "                                   ",
        "                                   ",
        "                                   ",
        "                                   ",
        "                                   ",
        "        H                 H        ",
        "                                   ",
        "                                   ",
        "                                   ",
        "                                   ",
        "                                   ",
        "                                   ",
        "  D                             D  ",
        " FFF          FFFFFFF          FFF ",
        " AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA "
    },
    {   // 13
        "                FFF                ",
        "                                   ",
        "                                   ",
        "                                   ",
        "                                   ",
        "                                   ",
        "                                   ",
        "                                   ",
        "             E       E             ",
        "             E       E             ",
        "             E       E             ",
        "             E       E             ",
        "             E       E             ",
        "             E       E             ",
        "             E       E             ",
        "            HE       EH            ",
        "             E       E             ",
        "             E       E             ",
        "                                   ",
        "                                   ",
        "                                   ",
        "                                   ",
        "                                   ",
        "                                   ",
        "                                   ",
        "                                   ",
        "                                   ",
        "        H                 H        ",
        "                                   ",
        "                                   ",
        "                                   ",
        "                                   ",
        "                                   ",
        "                                   ",
        "  D                             D  ",
        " FFF         FFFFFFFFF         FFF ",
        " AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA "
    },
    {   // 14: the front of the central column (the controller is in its 4th layer)
        "                FFF                ",
        "                 E                 ",
        "                 E                 ",
        "                 E                 ",
        "                 E                 ",
        "                 E                 ",
        "               F   F               ",
        "              E     E              ",
        "              E     E              ",
        "              E     E              ",
        "              E     E              ",
        "              E     E              ",
        "              E     E              ",
        "              E     E              ",
        "              E     E              ",
        "              E     E              ",
        "                                   ",
        "                                   ",
        "                FFF                ",
        "                                   ",
        "                                   ",
        "            H         H            ",
        "                                   ",
        "                                   ",
        "                                   ",
        "                                   ",
        "                                   ",
        "                                   ",
        "                FFF                ",
        "                                   ",
        "                                   ",
        "                                   ",
        "                                   ",
        "       H                   H       ",
        "                                   ",
        "                                   ",
        "                                   ",
        "                                   ",
        "                XXX                ",
        "                XXX                ",
        "  D             XXX             D  ",
        " FFF        FFFFFFFFFFF        FFF ",
        " AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA "
    },
    {   // 15
        "               F D F               ",
        "                 D                 ",
        "                 D                 ",
        "                 D                 ",
        "                 D                 ",
        "                 D                 ",
        "              F  D  F              ",
        "                 D                 ",
        "            D    D    D            ",
        "            D    D    D            ",
        "            D    D    D            ",
        "            D    D    D            ",
        "            D    D    D            ",
        "            D    D    D            ",
        "            D    D    D            ",
        "           DD    D    DD           ",
        "           D     D     D           ",
        "           D     D     D           ",
        "           D   F D F   D           ",
        "           D     C     D           ",
        "           D     C     D           ",
        "           D     C     D           ",
        "           D     C     D           ",
        "          DD     C     DD          ",
        "          D      C      D          ",
        "          D      C      D          ",
        "          D      C      D          ",
        "         DD      C      DD         ",
        "         D     FDCDF     D         ",
        "         D      DCD      D         ",
        "        DD      DCD      DD        ",
        "        D       DCD       D        ",
        "        D       DCD       D        ",
        "       DD      DDCDD      DD       ",
        "       D       D C D       D       ",
        "       D       D C D       D       ",
        "      DD       D C D       DD      ",
        "      D        D C D        D      ",
        "     DD MM     XDCDX     MM DD     ",
        "    DD  MI     XDCDX     IM  DD    ",
        " DDDD   MM     XDCDX     MM   DDDD ",
        "FFFD    MMFFFFFDDDDDFFFFFMM    DFFF",
        "AAAAAAAAMM  AAAXXXXXAAA  MMAAAAAAAA"
    },
    {   // 16
        "              F     F              ",
        "                                   ",
        "                                   ",
        "                                   ",
        "                                   ",
        "                                   ",
        "             F       F             ",
        "            E         E            ",
        "            E         E            ",
        "            E         E            ",
        "            E         E            ",
        "            E         E            ",
        "            E         E            ",
        "            E         E            ",
        "           EE         EE           ",
        "           EE         EE           ",
        "           E           E           ",
        "           E           E           ",
        "           E  F     F  E           ",
        "           E           E           ",
        "           E           E           ",
        "           E           E           ",
        "          EE           EE          ",
        "          EE           EE          ",
        "          E             E          ",
        "          E             E          ",
        "         EE             EE         ",
        "         EE             EE         ",
        "         E    FD   DF    E         ",
        "        EE     D   D     EE        ",
        "        EE     D   D     EE        ",
        "        E      D   D      E        ",
        "       EE      D   D      EE       ",
        "       EE      D   D      EE       ",
        "       E                   E       ",
        "      EE                   EE      ",
        "      EE                   EE      ",
        "     EE                     EE     ",
        "    EEE       XD   DX       EEE    ",
        "   EEE        XD   DX        EEE   ",
        "  EE          XD   DX          EE  ",
        "FFF         FFFDDDDDFFF         FFF",
        "AAAAAAAA    AAAXXXXXAAA    AAAAAAAA"
    },
    {   // 17: the middle, with the cable on top of the motor shaft
        "              FD   DF              ",
        "              ED   DE              ",
        "              ED   DE              ",
        "              ED   DE              ",
        "              ED   DE              ",
        "              ED   DE              ",
        "             F D   D F             ",
        "               D   D               ",
        "               D   D               ",
        "               D   D               ",
        "               D   D               ",
        "               D   D               ",
        "               D   D               ",
        "               D   D               ",
        "               D   D               ",
        "               D   D               ",
        "               D   D               ",
        "               D B D               ",
        "              FD - DF              ",
        "               C - C               ",
        "               C - C               ",
        "               C - C               ",
        "               C - C               ",
        "               C - C               ",
        "               C - C               ",
        "               C - C               ",
        "               C - C               ",
        "               C - C               ",
        "              FC - CF              ",
        "               C - C               ",
        "               C - C               ",
        "               C - C               ",
        "               C - C               ",
        "               C - C               ",
        "               C - C               ",
        "               C - C               ",
        "               C - C               ",
        "               C - C               ",
        "        MM    XC - CX    MM        ",
        "        MI    XC - CX    IM        ",
        "        MM    XC - CX    MM        ",
        "        MMFFFFFDDDDDFFFFFMM        ",
        "AAAAAAAAMM  AAAXXXXXAAA  MMAAAAAAAA"
    }
};

    // GTNH's extended elevator (STRUCTURE_PIECE_EXTENDED): a ring of 47 x 47 round the tower's foot, in its bottom five layers,
    // with twelve more module slots (three in the middle of every side). Written as the tower is: the slices from the front
    // to the middle one, each its rows from its highest block down; the tower stands in the middle of it.
    private static final String[][] SE_EXTENSION = {
    {   // 0
        "                    FFFFFFF                    ",
        "                    AAAAAAA                    "
    },
    {   // 1
        "                     M M M                     ",
        "                     M M M                     ",
        "                     M M M                     ",
        "                   FFM M MFF                   ",
        "                 AAAAM M MAAAA                 "
    },
    {   // 2
        "                     M M M                     ",
        "                     I I I                     ",
        "                     M M M                     ",
        "                   FFM M MFF                   ",
        "              AAAAAAAM M MAAAAAAA              "
    },
    {   // 3
        "                   FFF F FFF                   ",
        "            AAAAAAAAAA   AAAAAAAAAA            "
    },
    {   // 4
        "                  FFFF F FFFF                  ",
        "          AAAA   AAAAA   AAAAA   AAAA          "
    },
    {   // 5
        "                  FFFF F FFFF                  ",
        "        AAAA     AAAAA   AAAAA     AAAA        "
    },
    {   // 6
        "                  FFFFFFFFFFF                  ",
        "       AAA      AAAAAAAAAAAAAAA      AAA       "
    },
    {   // 7
        "      AAA       AA           AA       AAA      "
    },
    {   // 8
        "     AAA                               AAA     "
    },
    {   // 9
        "     AA                                 AA     "
    },
    {   // 10
        "    AA                                   AA    "
    },
    {   // 11
        "    AA                                   AA    "
    },
    {   // 12
        "   AA                                     AA   "
    },
    {   // 13
        "   AA                                     AA   "
    },
    {   // 14
        "  AA                                       AA  "
    },
    {   // 15
        "  AA                                       AA  "
    },
    {   // 16
        "  AA  AA                               AA  AA  "
    },
    {   // 17
        " AAAAAAA                               AAAAAAA "
    },
    {   // 18
        "    FFF                                 FFF    ",
        " AAAAAA                                 AAAAAA "
    },
    {   // 19
        " FFFFFF                                 FFFFFF ",
        " AAAAAA                                 AAAAAA "
    },
    {   // 20
        "FFFFFFF                                 FFFFFFF",
        "AAAAAAA                                 AAAAAAA"
    },
    {   // 21
        " MM                                         MM ",
        " MI                                         IM ",
        " MM                                         MM ",
        "FMMFFFF                                 FFFFMMF",
        "AMMAAAA                                 AAAAMMA"
    },
    {   // 22
        "F     F                                 F     F",
        "A     A                                 A     A"
    },
    {   // 23
        " MM                                         MM ",
        " MI                                         IM ",
        " MM                                         MM ",
        "FMMFFFF                                 FFFFMMF",
        "AMM   A                                 A   MMA"
    }
};

    public static final int WIDTH = 35;
    public static final int HEIGHT = 43;
    public static final int DEPTH = 35;
    // the controller: the front centre of the central column, in the 4th layer (column, row from the top, slice)
    private static final int[] CONTROLLER = { 17, 39, 14 };
    // the extension: how far it reaches out past the tower on every side, and how many of the bottom layers it is in
    private static final int REACH = 6;
    private static final int RING_HEIGHT = 5;

    private static String blank(int width) {
        return " ".repeat(width);
    }

    // A piece's slices in full, front to back, every slice `height` rows from the top down: the table holds the front half
    // and the middle slice, and of a slice only the rows from its highest block down
    private static String[][] unfold(String[][] table, int depth, int height, int width) {
        String blank = blank(width);
        String[][] slices = new String[depth][];
        for (int c = 0; c < depth; c++) {
            String[] rows = table[c < table.length ? c : depth - 1 - c];
            String[] slice = new String[height];
            for (int b = 0; b < height; b++) slice[b] = b < height - rows.length ? blank : rows[b - (height - rows.length)];
            slices[c] = slice;
        }
        return slices;
    }

    // The tower's slices with the controller in its place; extended: the tower in the middle of the extension's ring
    public static String[][] slices(boolean extended) {
        String[][] tower = unfold(SE_MAIN, DEPTH, HEIGHT, WIDTH);
        String row = tower[CONTROLLER[2]][CONTROLLER[1]];
        tower[CONTROLLER[2]][CONTROLLER[1]] = row.substring(0, CONTROLLER[0]) + 'S' + row.substring(CONTROLLER[0] + 1);
        if (!extended) return tower;
        int width = WIDTH + 2 * REACH;
        int depth = DEPTH + 2 * REACH;
        String[][] ring = unfold(SE_EXTENSION, depth, RING_HEIGHT, width);
        String[][] slices = new String[depth][HEIGHT];
        for (int c = 0; c < depth; c++) {
            for (int b = 0; b < HEIGHT; b++) {
                StringBuilder line = new StringBuilder(width);
                for (int a = 0; a < width; a++) {
                    // the tower's block where it has one, else the ring's (they agree where both have one)
                    char block = c >= REACH && c < REACH + DEPTH && a >= REACH && a < REACH + WIDTH
                            ? tower[c - REACH][b].charAt(a - REACH) : ' ';
                    if (block == ' ' && b >= HEIGHT - RING_HEIGHT) {
                        block = ring[c][b - (HEIGHT - RING_HEIGHT)].charAt(a);
                    }
                    line.append(block);
                }
                slices[c][b] = line.toString();
            }
        }
        return slices;
    }
}
