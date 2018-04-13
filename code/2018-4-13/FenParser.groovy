// Copyright John Hurst
// john.b.hurst@gmail.com
// 2018-04-13

import groovy.transform.ToString
import groovy.transform.TupleConstructor

/**
 * Parser for Chess poistion in Forsyth-Edwards Notation.
 * @link https://en.wikipedia.org/wiki/Forsyth%E2%80%93Edwards_Notation
 */
class FenParser {
  @TupleConstructor
  @ToString
  static class ChessPiece {
    Piece piece
    Color color
    int file
    int rank
  }

  Collection<ChessPiece> parse(String s) {
    List<String> fields = s.split(" ")
    String placement = fields[0]
    List<String> ranks = placement.split("/")
    assert ranks.size() == 8
    Collection<ChessPiece> result = []
    ranks.eachWithIndex {rank, r ->
      int f = 0
      while (f < 8) {
        def c = rank[f]
        switch (c) {
          case "K": result << new ChessPiece(Piece.KING, Color.WHITE, 1+f++, 8-r); break
          case "k": result << new ChessPiece(Piece.KING, Color.BLACK, 1+f++, 8-r); break
          case "Q": result << new ChessPiece(Piece.QUEEN, Color.WHITE, 1+f++, 8-r); break
          case "q": result << new ChessPiece(Piece.QUEEN, Color.BLACK, 1+f++, 8-r); break
          case "R": result << new ChessPiece(Piece.ROOK, Color.WHITE, 1+f++, 8-r); break
          case "r": result << new ChessPiece(Piece.ROOK, Color.BLACK, 1+f++, 8-r); break
          case "B": result << new ChessPiece(Piece.BISHOP, Color.WHITE, 1+f++, 8-r); break
          case "b": result << new ChessPiece(Piece.BISHOP, Color.BLACK, 1+f++, 8-r); break
          case "N": result << new ChessPiece(Piece.KNIGHT, Color.WHITE, 1+f++, 8-r); break
          case "n": result << new ChessPiece(Piece.KNIGHT, Color.BLACK, 1+f++, 8-r); break
          case "P": result << new ChessPiece(Piece.PAWN, Color.WHITE, 1+f++, 8-r); break
          case "p": result << new ChessPiece(Piece.PAWN, Color.BLACK, 1+f++, 8-r); break
          case ~/[1-8]/: f += c as int; break
          default: throw InvalidArgumentException("Unrecognised character [$c] in input")
        }
      }
    }
    return result
  }
}

