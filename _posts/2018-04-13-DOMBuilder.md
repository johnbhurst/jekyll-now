---
layout: post
title: Groovy DOMBuilder for programmatic DOM creation
description: Programmatic XML DOM creation with Groovy MarkupBuilder, DOMBuilder, StreamingDOMBuilder.
category: groovy
tags: [groovy, xml, namespaces, svg, apache batik, markupbuilder, dombuilder, streamingdombuilder]
---

Groovy contains quite a variety of classes for working with XML.

The main Groovy ones are XmlParser, XmlStreamer, MarkupBuilder and StreamingMarkupBuilder.

These classes provide a distinctly Groovy-flavored approach to XML.

But Groovy also provides some classes that help with mainstream Java XML APIs.
Most notable are `DOMBuilder` and `StreamingDOMBuilder`.

In one sense, `DOMBuilder` is well-documented. Books and online samples illustrate the use of `DOMBuilder` to parse XML files and produce
  a DOM object.
A typical example from [*Groovy In Action*](https://www.manning.com/books/groovy-in-action-second-edition) is:

``` groovy
def doc = DOMBuilder.parse(new FileREader('data/plan.xml'))
```

But `DOMBuilder` can also be used as a builder, like `MarkupBuilder`. This is not very well-documented. [*Groovy In Action*](https://www.manning.com/books/groovy-in-action-second-edition) hints at it, but does not spell it out, or provide examples.
[MrHaki's excellent Groovy blog](http://mrhaki.blogspot.com) does not mention it.
This feature is easy to miss, given all the examples of `DOMBuilder.parse()`.

There is also `StreamingDOMBuilder`, which adds extra features like namespace support, but is much less documented than `DOMBuilder`.

This post shows some usages of `DOMBuilder` and `StreamingDOMBuilder`, along with some gotchas you may encounter.

# Motivation

Why do we care about `DOMBuilder`? Why don't we just use Groovy's more fluent classes such as `MarkupBuilder` and `StreamingMarkupBuilder`?

The main case for `DOMBuilder` is when we want to integrate with other Java APIs that use the standard `Document`, `Element`, `Node` etc classes in the `org.w3c.dom` package.
Many APIs do this.
Some examples are:

* Databases that store XMLTypes (e.g. Oracle).
* XSLT APIs.
* Apache Batik SVG (see below).

When we want to interact with these APIs, we need a DOM object.

# Example: SVG

We will generate [SVG](https://www.w3.org/TR/SVG11/) files for a chessboard, like this one:

![Starting Chessboard](/code/2018-04-13/startingboard.svg)

(Code [here](https://github.com/johnbhurst/johnbhurst.github.io/blob/master/code/2018-04-13/startingboard.svg).)

We'll generate this image using Groovy using various XML builders.

# MarkupBuilder

First we'll do the one that is the most familiar and well-documented: MarkupBuilder.

The main piece of code is this:

``` groovy
new MarkupBuilder().svg(xmlns: "http://www.w3.org/2000/svg", viewBox: "-5 -5 810 810", width: "800", height: "800") {
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
```

(The full code is [here](https://github.com/johnbhurst/johnbhurst.github.io/blob/master/code/2018-04-13/makefen_svg_markupbuilder.groovy).)

There is not much to see here. The main thing to notice is that the namespaces are not properly supported.
The default namespace declaration for `xmlns` in the top-level `<svg>` element defines a default namespace,
but `MarkupBuilder` treats this like any other attribute and is not aware of the namespaces.
It doesn't matter, because all the content is immediately serialized and the resulting XML is valid.

# DOMBuilder

Now we will show how to create a DOM object programmatically using `DOMBuilder`.

The main code uses the same closure as with `MarkupBuilder`, but executes it using `DOMBuilder` instead:

``` groovy
def dom = DOMBuilder.newInstance().svg(xmlns: "http://www.w3.org/2000/svg", viewBox: "-5 -5 810 810", width: "800", height: "800") {
  // XML-generating closure same as before ...
}

println XmlUtil.serialize(dom)
```

(The full code is [here](https://github.com/johnbhurst/johnbhurst.github.io/blob/master/code/2018-04-13/makefen_svg_dombuilder.groovy).)

Here we use `XmlUtil.serialize()` to write out the resulting XML, because `org.w3c.dom.Element` objects don't know how to serialize themselves.

As with `MarkupBuilder`, the lack of namespace support does not matter, because we are serializing immediately, and the resulting XML is valid.

# DOMBuilder namespace issues

Now we will see how to use a DOM object from `DOMBuilder` with another API.

In this case, we want to convert our SVG document into a PNG image.
We can use the [Apache Batik project](https://xmlgraphics.apache.org/batik/) for this:

``` groovy
def doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument()
def dom = new DOMBuilder(doc).svg(xmlns: "http://www.w3.org/2000/svg", viewBox: "-5 -5 810 810", width: "800", height: "800") {
  // XML-generating closure same as before ...
}
doc.appendChild(dom)

File file = new File(args[1])
assert !file.exists(), "file $file.name already exists"
PNGTranscoder transcoder = new PNGTranscoder()
TranscoderInput input = new TranscoderInput(doc)
try {
  file.withOutputStream {os ->
    TranscoderOutput output = new TranscoderOutput(os)
    transcoder.transcode(input, output)
  }
}
catch (Exception ex) {
  println "Transcoding failed: $ex.message"
  ex.printStackTrace(System.out)
}
```

(The full code is [here](https://github.com/johnbhurst/johnbhurst.github.io/blob/master/code/2018-04-13/makefen_png_dombuilder.groovy).)

`DOMBuilder` with a closure returns a `org.w3c.dom.Element`.
Unfortunately, this `Element`'s `ownerDocument` is not properly linked up with the element.
We correct for this with the line:

``` groovy
doc.appendChild(dom)
```

After this, we have a `org.w3c.dom.Document` with an SVG document model, and we try to use it with Batik.

But we get an error:

```
Transcoding failed: org.apache.batik.dom.GenericElement cannot be cast to org.w3c.dom.svg.SVGSVGElement
java.lang.ClassCastException: org.apache.batik.dom.GenericElement cannot be cast to org.w3c.dom.svg.SVGSVGElement
	at org.apache.batik.anim.dom.SVGOMDocument.getRootElement(SVGOMDocument.java:235)
	at org.apache.batik.transcoder.SVGAbstractTranscoder.transcode(SVGAbstractTranscoder.java:193)
	at org.apache.batik.transcoder.image.ImageTranscoder.transcode(ImageTranscoder.java:92)
```

Why is this?

The reason is that Batik has not been able to read the DOM as an SVG model because the namespace information is not present.

We can add some code to observe this:

``` groovy
def elt = doc.documentElement
println "Element class: [${elt.class}]"
println "localName = [$elt.localName]"
println "name = [$elt.name]"
println "namespaceURI: [$elt.namespaceURI]"
println "nodeName = [$elt.nodeName]"
println "prefix = [$elt.prefix]"
println "tagName = [$elt.tagName]"
```

The output is:

```
Element class: [class com.sun.org.apache.xerces.internal.dom.ElementImpl]
localName = [null]
name = [svg]
namespaceURI: [null]
nodeName = [svg]
prefix = [null]
tagName = [svg]
```

Sure enough, there is no namespace.

# StreamingDOMBuilder

We can fix this by switching to `StreamingDOMBuilder`, which supports namespaces:

``` groovy
def builder = new StreamingDOMBuilder()
def xml = builder.bind {
  namespaces << ["": "http://www.w3.org/2000/svg"]
  svg(viewBox: "-5 -5 810 810", width: "800", height: "800") {
    // XML-generating closure same as before ...
  }
}

def doc = xml()

File file = new File(args[1])
assert !file.exists(), "file $file.name already exists"
PNGTranscoder transcoder = new PNGTranscoder()
TranscoderInput input = new TranscoderInput(doc)
file.withOutputStream {os ->
  TranscoderOutput output = new TranscoderOutput(os)
  transcoder.transcode(input, output)
}
```

(The full code is [here](https://github.com/johnbhurst/johnbhurst.github.io/blob/master/code/2018-04-13/makefen_png_streamingdombuilder.groovy).)

Now when we run the code, Batik works, and we get a PNG file.

We can add the same debugging `println` statements as before, and confirm that the XML namespace information is correct in the `Document`:

```
Element class: [class com.sun.org.apache.xerces.internal.dom.ElementNSImpl]
localName = [svg]
name = [svg]
namespaceURI: [http://www.w3.org/2000/svg]
nodeName = [svg]
prefix = [null]
tagName = [svg]
```

Now we have a namespace-aware `Element`, and the `namespaceURI` property is set with the SVG namespace.

# What next?

Although `StreamingDOMBuilder` works great for elements with namespaces, I've found that it does not support attributes with namespaces.

Attributes with namespaces are much more rare than elements with namespaces, but they do come up.

Some common examples that I have come across are:

* The `xs:type` attribute using the `http://www.w3.org/2001/XMLSchema` namespace.
* The `xl:link` attribute using the `https://www.w3.org/1999/xlink` namespace. This one is used in SVG for example, when referencing symbols defined in the `<defs>` element.

I have not found a way to generate DOM objects with namespaced attributes programatically in Groovy.

The best workaround I have is to use `StreamingMarkupBuilder`, serialize the XML, and re-read using `DOMBuilder` with namespace awareness enabled:

``` groovy
def builder = new StreamingMarkupBuilder()
def xml = builder.bind {
  namespaces << ["": "http://www.w3.org/2000/svg", "xl": "http://www.w3.org/1999/xlink"]
  svg(viewBox: "-5 -5 810 810", width: "800", height: "800") {
    // XML-generating closure
  }
}
def doc = DOMBuilder.parse(new StringReader(xml.toString()), false, true)
// proceed with DOM object
```

Perhaps I will have more to say about this later.
