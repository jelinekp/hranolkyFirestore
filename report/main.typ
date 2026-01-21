#set page(
  paper: "a4",
  margin: (top: 2.5cm, bottom: 2.5cm, left: 2.5cm, right: 2.5cm),
  footer: context {
    if counter(page).get().first() > 1 [
      #set text(size: 11pt, fill: luma(80))
      #grid(
        columns: (1fr, 1fr),
        [], align(right, counter(page).display()),
      )
    ]
  },
)

#set text(
  lang: "en",
  // font: "Linux Libertine", // Commented out to use default
  size: 11pt,
)

// Code block styling
#show raw.where(block: true): block.with(
  fill: rgb(240, 240, 240),
  inset: 10pt,
  radius: 4pt,
  stroke: luma(200),
)

#show raw: set text(font: "DejaVu Sans Mono", size: 8pt)
#show figure.where(kind: raw): set figure(supplement: [Ukázka])
#show link: set text(fill: blue.darken(40%))
#show outline.entry: set text(fill: blue.darken(50%))
#show ref: set text(fill: blue.darken(40%))

#show heading.where(level: 1): it => {
  pagebreak(weak: true)
  it
}

// Title Page
#align(center + horizon)[
  #image("media/logo-uantwerpen-be-en-rgb-pos.pdf", width: 40%)
  #v(2em)

  #text(2em, weight: "bold")[Warehouse App refactoring using Normalized System Theory]

  #v(2em)

  #text(1.2em)[Pavel Jelínek]

  #v(0.5em)
  DA-SEA

  #v(2em)
  #datetime.today().display("[day]. [month]. [year]")
]

#pagebreak()

// Table of Contents
#outline(depth: 2, indent: auto)
#pagebreak()

// Content
#include "sections/01_introduction.typ"

#set heading(numbering: "1.")
#include "sections/02_theory.typ"
#include "sections/03_current.typ"
#include "sections/04_refactored.typ"

#set heading(numbering: none)
#include "sections/08_conclusion.typ"

#bibliography("references.bib")
