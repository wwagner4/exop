package exop

@Suppress("EnumEntryName")
class Font {

    data class Def(
        val fontName: String,
        val import: String? = null,
    )

    @Suppress("unused")
    enum class Family(val def: Def) {
        serif(Def("serif")),
        sansSerif(Def("sans-serif")),
        monospace(Def("monospace")),
        cursive(Def("cursive")),
        fantasy(Def("fantasy")),
        delius(
            Def(
                "'Delius', cursive",
                "@import url('https://fonts.googleapis.com/css2?family=Delius&display=swap');"
            )
        ),
        league(
            Def(
                "'League Script', cursive",
                "@import url('https://fonts.googleapis.com/css2?family=League+Script&display=swap');"
            )
        ),
        eagle(
            Def(
                "'Eagle Lake', cursive",
                "@import url('https://fonts.googleapis.com/css2?family=Eagle+Lake&display=swap');"
            )
        ),
        slab(
            Def(
                "'Antic Slab', serif",
                "@import url('https://fonts.googleapis.com/css2?family=Antic+Slab&display=swap');"
            )
        ),
        cabin(
            Def(
                "'Cabin Sketch', cursive",
                "@import url('https://fonts.googleapis.com/css2?family=Cabin+Sketch:wght@400;700&display=swap');"
            )
        ),
        dmSerif(
            Def(
                "'DM Serif Text', serif",
                "@import url('https://fonts.googleapis.com/css2?family=DM+Serif+Text:ital@0;1&display=swap');"
            )
        ),
        turretRoad(
            Def(
                "'Turret Road', cursive",
                "@import url('https://fonts.googleapis.com/css2?family=Turret+Road:wght@500&display=swap');"
            )
        ),

    }


}