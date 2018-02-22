package de.fatox.meta.graphics.model

object MDXConverter {
//    private val builder: StringBuilder = StringBuilder()
//
//    private lateinit var mdx: MDX
//    val json = Json(JsonWriter.OutputType.json)
//
//    val stringWriter = StringWriter()
//    private val writer = JsonWriter(stringWriter)
//
//    fun convert(file: FileHandle): String {
//        mdx = MDX(file.file())
//        builder.setLength(0)
//        genMainBlock()
//        return json.prettyPrint(stringWriter.toString())
//    }
//
//    private fun genMainBlock() {
//        json.setWriter(writer)
//        json.writeObjectStart()
//        genVersion()
//        genId()
//        genMeshes()
//        genMaterials()
//        genNodes()
//        genAnimations()
//        json.writeObjectEnd()
//    }
//
//    private fun genId() {
//        json.writeValue("id", mdx.modelInfoChunk.get().name.trim() + "_conv")
//    }
//
//    private fun genVersion() {
//        json.writeArrayStart("version")
//        json.writeValue(0)
//        json.writeValue(1)
//        json.writeArrayEnd()
//    }
//
//    private var count = 0
//
//    private fun genMeshes() {
//        json.writeArrayStart("meshes")
//        mdx.geosetChunks.forEach({
//            it.geosets.forEach({
//                genMesh(it)
//                count++
//            })
//        })
//        json.writeArrayEnd()
//    }
//
//    private fun genMesh(geoset: Geoset) {
//        json.writeObjectStart()
//        json.writeArrayStart("attributes")
//        json.writeValue("POSITION")
//        json.writeValue("NORMAL")
//        json.writeValue("TEXCOORD0")
//        json.writeArrayEnd()
//
//        genVertices(geoset)
//        genParts(geoset)
//        json.writeObjectEnd()
//    }
//
//    private var partCount = 0
//
//    private fun genVertices(geoset: Geoset) {
//        json.writeArrayStart("vertices")
//        for (i in 0 until geoset.vertexChunk.vertices.size) {
//            val vertex = geoset.vertexChunk.vertices[i]
//            val normal = geoset.vertexNormalChunk.vertices[i]
//            val texCoords = geoset.texCoordSetChunk.texCoordSets[0].texCoords[i]
//            json.writeValue(vertex.pos.x.toDouble())
//            json.writeValue(vertex.pos.z.toDouble())
//            json.writeValue(vertex.pos.y.toDouble())
//            json.writeValue(normal.pos.x.toDouble())
//            json.writeValue(normal.pos.z.toDouble())
//            json.writeValue(normal.pos.y.toDouble())
//            json.writeValue(texCoords.pos.y.toDouble())
//            json.writeValue(texCoords.pos.x.toDouble())
//        }
//        json.writeArrayEnd()
//    }
//
//    private fun genParts(geoset: Geoset) {
//        json.writeArrayStart("parts")
//        json.writeObjectStart()
//        json.writeValue("id", "geosetpart_$partCount")
//        json.writeValue("type", "TRIANGLES")
//        json.writeArrayStart("indices")
//
//        geoset.faceChunk.faces.forEach({
//            json.writeValue(it.`val`.toDouble())
//        })
//        partCount++
//        json.writeArrayEnd()
//        json.writeObjectEnd()
//        json.writeArrayEnd()
//    }
//
//    private fun genMaterials() {
//        json.writeArrayStart("materials")
//        matCount = 0
//        mdx.materialChunks.first().materials.forEach({
//            genMaterial(it)
//            matCount++
//        })
//        json.writeArrayEnd()
//    }
//
//    private var matCount = 0
//
//
//    private fun genMaterial(it: Material) {
//        json.writeObjectStart()
//        json.writeValue("id", "Material_$matCount")
//        json.writeArrayStart("ambient")
//        json.writeValue(0.0)
//        json.writeValue(0.0)
//        json.writeValue(0.0)
//        json.writeArrayEnd()
//
//        json.writeArrayStart("diffuse")
//        json.writeValue(0.0)
//        json.writeValue(0.0)
//        json.writeValue(0.0)
//        json.writeArrayEnd()
//
//        json.writeArrayStart("emissive")
//        json.writeValue(0.0)
//        json.writeValue(0.0)
//        json.writeValue(0.0)
//        json.writeArrayEnd()
//
//        json.writeValue("opacity", 0.0)
//
//        json.writeArrayStart("specular")
//        json.writeValue(0.0)
//        json.writeValue(0.0)
//        json.writeValue(0.0)
//        json.writeArrayEnd()
//
//        json.writeValue("shininess", 0.0)
//
//        genTextures(it)
//
//        json.writeObjectEnd()
//    }
//
//    private var texCount = 0
//
//    private fun genTextures(it: Material) {
//        json.writeArrayStart("textures")
//        val textureSet = HashSet<Int>()
//        it.layers.forEach({
//            textureSet.add(it.textureId.toInt())
//        })
//        texCount = 0
//        textureSet.forEach({
//            genTexture(it)
//            texCount++
//        })
//
//        json.writeArrayEnd()
//    }
//
//    private fun genTexture(textureId: Int) {
//        json.writeObjectStart()
//        json.writeValue("id", "Texture_$textureId")
//        json.writeValue("filename", mdx.textureChunks.first().textures[textureId].fileName)
//        json.writeValue("type", "DIFFUSE")
//        json.writeObjectEnd()
//    }
//
//    private fun genNodes() {
//        json.writeArrayStart("nodes")
//        mdx.materialChunks.first().materials.forEach({
//            it.layers.forEach({
//                genNode(it)
//            })
//        })
//        json.writeArrayEnd()
//    }
//
//    private fun genNode(it: Layer) {
//        json.writeObjectStart()
//        json.writeValue("id", "node")
//        genParts()
//        json.writeObjectEnd()
//    }
//
//    private fun genParts() {
//        json.writeArrayStart("parts")
//        json.writeObjectStart()
//        json.writeValue("meshpartid", "node")
//        json.writeValue("materialid", "node")
//        json.writeValue("uvMapping", "[[0]]")
//        json.writeObjectEnd()
//        json.writeArrayEnd()
//    }
//
//    private fun genAnimations() {
//        json.writeArrayStart("animations")
//        json.writeArrayEnd()
//    }

}
