# gt-dxf
DXF feature reader implementation for modern releases of GeoTools.

This library is forked from the old, unsupported gt-dxf module, and since then the libary has seen multiple features added.
What is most important, the library is kept up-to-date with GeoTools developments. Currently, the library provides quite
feature complete DXF read-only support in GeoTools.

## Building and installing

The library uses Maven as the build system. It can simply be built and installed in the local Maven repository by issuing command:

    $ mvn install

There are no tests in the code, unfortunately.

## Usage

Basically the software provides a feature store factory, that can be used for reading the DXF data, along with CRS information and
possible _additional_ affine transform for the data (example in Kotlin):

```java
    import org.geotools.data.dxf.DXFDataStoreFactory

    val factory = DXFDataStoreFactory()
    val map = HashMap<String, Any?>()
    // Source CRS
    map.put(DXFDataStoreFactory.PARAM_SRS.key, "EPSG:)
    // Target CRS
    map.put(DXFDataStoreFactory.PARAM_TARGET_SRS.key, "EPSG:4326")
    map.put(DXFDataStoreFactory.PARAM_AFFINE_TRANSFORM.key, AffineTransform())
    // URL to the DXF file
    map.put(DXFDataStoreFactory.PARAM_URL.key, url)
    val store = factory.createDataStore(map)
    val featureSource = store.getFeatureSource("")
```

Afterwards, the layer can be added to MapContent:

```java
    content.addLayer(FeatureLayer(featureSource, createStyle(), name))
```
    

All the DXF information for styling is exposed. The following code can be used for creating Style for FeatureLayer
that properly displays the DXF with all the right colors and so on:

```java
    import org.geotools.data.FeatureSource
    import org.geotools.data.FileDataStore
    import org.geotools.data.dxf.DXFDataStoreFactory
    import org.geotools.data.simple.SimpleFeatureCollection
    import org.geotools.factory.CommonFactoryFinder
    import org.geotools.filter.AttributeExpressionImpl
    import org.geotools.map.FeatureLayer
    import org.geotools.map.MapContent
    import org.geotools.styling.Displacement
    import org.geotools.styling.FeatureTypeStyle
    import org.geotools.styling.PointPlacementImpl
    import org.geotools.styling.Style
    import org.geotools.styling.StyleBuilder
    import org.geotools.styling.TextSymbolizer
    import org.opengis.feature.simple.SimpleFeature
    import org.opengis.feature.simple.SimpleFeatureType
    import org.opengis.filter.expression.Expression
    import org.opengis.referencing.crs.CoordinateReferenceSystem
    import org.opengis.style.GraphicalSymbol

    // Class definition etc.

    val FILTER_FACTORY = CommonFactoryFinder.getFilterFactory(null)
    val STYLE_FACTORY = CommonFactoryFinder.getStyleFactory(null)

    /** Create a style and rule for point elements. */
    fun createPointStyle(): FeatureTypeStyle {
        val color = AttributeExpressionImpl("color")

        // Create stroke with the specific color and with of 1 px
        val stroke = STYLE_FACTORY.createStroke(color, FILTER_FACTORY.literal(1))

        // Symbolize points with
        val symbols = ArrayList<GraphicalSymbol>()
        symbols.add(STYLE_FACTORY.mark(FILTER_FACTORY.literal("circle"), null, stroke))
        val circle = STYLE_FACTORY.graphic(symbols, null, FILTER_FACTORY.literal(8), null, null, null)
        val sym = STYLE_FACTORY.createPointSymbolizer(circle, null)
        sym.unitOfMeasure = SI.METRE // Make sure the diameter is in meters

        // Create rule that renders visible DXFPoint elements using the style
        val rule = STYLE_FACTORY.createRule()
        rule.filter = FILTER_FACTORY.and(FILTER_FACTORY.notEqual(AttributeExpressionImpl("visible"),
                FILTER_FACTORY.literal(0)), FILTER_FACTORY.equals(AttributeExpressionImpl("class"),
                FILTER_FACTORY.literal("DXFPoint")))
        rule.symbolizers().add(sym)
        return STYLE_FACTORY.createFeatureTypeStyle(arrayOf(rule))
    }

    /** Create a style and rule for line elements. */
    protected fun createLineStyle(): FeatureTypeStyle {
        val color = AttributeExpressionImpl("color")

        // Create stroke with the defined color and thickness from so named attribute
        val stroke = STYLE_FACTORY.createStroke(
                color, AttributeExpressionImpl("thickness"))
        val sym = STYLE_FACTORY.createLineSymbolizer(stroke, null)

        // Render all visible line type features
        val rule = STYLE_FACTORY.createRule()
        rule.filter = FILTER_FACTORY.notEqual(AttributeExpressionImpl("visible"),
                FILTER_FACTORY.literal(0))
        rule.symbolizers().add(sym)
        return STYLE_FACTORY.createFeatureTypeStyle(arrayOf(rule))
    }

    /** Create a style and rule for polygon elements. */
    fun createPolygonStyle(): FeatureTypeStyle {
        val color = AttributeExpressionImpl("color")

        // Create stroke with the defined color and thickness from so named attribute
        val stroke = STYLE_FACTORY.createStroke(color,
                AttributeExpressionImpl("thickness"))
        val sym = STYLE_FACTORY.createPolygonSymbolizer(stroke, null, null)

        // Render all visible polygon type features
        val rule = STYLE_FACTORY.createRule()
        rule.filter = FILTER_FACTORY.notEqual(AttributeExpressionImpl("visible"),
                FILTER_FACTORY.literal(0))
        rule.symbolizers().add(sym)
        return STYLE_FACTORY.createFeatureTypeStyle(arrayOf(rule))
    }

    /** Create a style and rule for text elements. */
    fun createTextStyle(): FeatureTypeStyle {
        val color = AttributeExpressionImpl("color")

        val styleBuilder = StyleBuilder()

        // Create an anchor that honors the horizontal and vertical text alignment of the element
        val anchor = AnchorPoint()
        anchor.anchorPointX = AttributeExpressionImpl("align1")
        anchor.anchorPointY = AttributeExpressionImpl("align2")

        // Construct the label placement object with right rotation
        val labelPlacement = PointPlacementImpl(FILTER_FACTORY)
        labelPlacement.rotation = FILTER_FACTORY.multiply(FILTER_FACTORY.literal(-1), AttributeExpressionImpl("rotation"))
        labelPlacement.setAnchorPoint(anchor)
        labelPlacement.setDisplacement(Displacement.DEFAULT)

        // Use sans serif font and get text height (as meters) from the height attribute
        val font = styleBuilder.createFont(FILTER_FACTORY.literal("SansSerif"),
                FILTER_FACTORY.literal(1), FILTER_FACTORY.literal(1),
                AttributeExpressionImpl("height"))

        // Create fill and the actual symbolizer
        val fill = STYLE_FACTORY.createFill(color)
        val sym = STYLE_FACTORY.createTextSymbolizer(fill,
                arrayOf(font), null, AttributeExpressionImpl("text"), labelPlacement, null)

        // Finally set options so that labels can overlap and they are not moved
        // from their specified locations and set units to meters
        val options = sym.options
        options.put(TextSymbolizer.CONFLICT_RESOLUTION_KEY, "false")
        options.put(TextSymbolizer.GROUP_KEY, "false")
        options.put(TextSymbolizer.AUTO_WRAP_KEY, "-1")
        options.put(TextSymbolizer.GOODNESS_OF_FIT_KEY, "-1")
        options.put(TextSymbolizer.MAX_DISPLACEMENT_KEY, "0")
        options.put(TextSymbolizer.ALLOW_OVERRUNS_KEY, "true")
        options.put(TextSymbolizer.MIN_GROUP_DISTANCE_KEY, "-1")
        options.put(TextSymbolizer.SPACE_AROUND_KEY, "-1")
        options.put(TextSymbolizer.LABEL_ALL_GROUP_KEY, "true")
        options.put(TextSymbolizer.PARTIALS_KEY, "true")
        options.put(TextSymbolizer.REMOVE_OVERLAPS_KEY, "false")
        sym.unitOfMeasure = SI.METRE

        // Create the rule to render visible DXFText and DXFMtext elements
        val rule = STYLE_FACTORY.createRule()
        rule.filter = FILTER_FACTORY.and(
                FILTER_FACTORY.notEqual(AttributeExpressionImpl("visible"), FILTER_FACTORY.literal(0)),
                FILTER_FACTORY.or(
                        FILTER_FACTORY.equals(AttributeExpressionImpl("class"), FILTER_FACTORY.literal("DXFText")),
                        FILTER_FACTORY.equals(AttributeExpressionImpl("class"), FILTER_FACTORY.literal("DXFMText"))
                )
        )
        rule.symbolizers().add(sym)
        return STYLE_FACTORY.createFeatureTypeStyle(arrayOf(rule))
    }

    /** Constructs the whole style for the layer. */
    fun createStyle(): Style {
        val s = STYLE_FACTORY.createStyle()

        // Add other style definitions
        s.featureTypeStyles().add(createPointStyle())
        s.featureTypeStyles().add(createLineStyle())
        s.featureTypeStyles().add(createPolygonStyle())
        s.featureTypeStyles().add(createTextStyle())
        return s
    }
```
