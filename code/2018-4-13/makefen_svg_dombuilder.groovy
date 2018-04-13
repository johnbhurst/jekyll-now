// Copyright 2018 John Hurst
// john.b.hurst@gmail.com
// 2018-04-13

// Generate SVG chessboard from FEN string.
// Example:
// groovy makefen_svg_dombuilder.groovy rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR
// generates starting position.

// This variant illustrates how DOMBuilder can be used like MarkupBuilder with XML DSL with closures.
// It uses namespace prefixes but without true namespace awareness.
// For this purpose namespace awareness is not required, because the XML serializes correctly.

import groovy.xml.DOMBuilder
import groovy.xml.XmlUtil

def positions = new FenParser().parse(args[0])

Map<Piece, String> PIECE_CHARS = [
  (Piece.KING): "\u265A",
  (Piece.QUEEN): "\u265B",
  (Piece.ROOK): "\u265C",
  (Piece.BISHOP): "\u265D",
  (Piece.KNIGHT): "\u265E",
  (Piece.PAWN): "\u265F",
]

def dom = DOMBuilder.newInstance().svg(xmlns: "http://www.w3.org/2000/svg", viewBox: "-5 -5 810 810", width: "800", height: "800") {
  defs {
    style(type: "text/css", """
      .piece { font-size: 90px; font-weight: 400; }
      .blackp { fill: black; }
      .whitep { fill: white; stroke: black; stroke-width: 2px;}
    """)
  }
  def square = {row, col ->
    def x = (col - 1) * 100
    def y = (row - 1) * 100
    def fill = (row+col) % 2 == 0 ? "#ddd" : "#999"
    rect(x: "$x", y: "$y", width: "100", height: "100", stroke: "black", "stroke-width": "3", fill: "$fill")
  }
  for (int row in 1..8) {
    for (int col in 1..8) {
      square(row, col)
    }
  }
  rect(x: "0", y: "0", width: "800", height: "800", stroke: "black", "stroke-linecap": "round", "stroke-linejoin": "round", "stroke-width": "8", fill: "none")
  positions.each {p ->
    def x = 100 * (p.file - 1) + 5
    def y = 100 * (8 - p.rank) + 80
    def color = p.color == Color.WHITE ? "whitep" : "blackp"
    def ch = PIECE_CHARS[p.piece]
    text {
      tspan(x: "$x", y: "$y", class: "$color piece", ch)
    }
  }
}

println XmlUtil.serialize(dom)
