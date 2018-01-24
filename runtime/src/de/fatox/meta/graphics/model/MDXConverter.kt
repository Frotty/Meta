package de.fatox.meta.graphics.model

import com.badlogic.gdx.files.FileHandle
import net.moonlightflower.wc3libs.misc.model.MDX
import net.moonlightflower.wc3libs.misc.model.mdx.Geoset

object MDXConverter {
    private val builder: StringBuilder = StringBuilder()

    private lateinit var mdx: MDX

    fun convert(file: FileHandle): String {
        mdx = MDX(file.file())
        builder.setLength(0)

        genMainBlock()

        return builder.toString()
    }

    private fun genMainBlock() {
        builder.append("{\n")
        genVersion()
        genId()
        genMeshes()
        builder.append("}\n")
    }

    private var count = 0

    private fun genMeshes() {
        builder.append("\t\"meshes\": [\n")
        mdx.geosetChunks.forEach({
            it.geosets.forEach({
                genMesh(it)
                count++
            })
        })
        builder.append("\t]\n")
    }

    private fun genMesh(geoset: Geoset) {
        builder.append("\t\t{\n")
        builder.append("\t\t\t\"attributes\": [\"POSITION\", \"NORMAL\", \"TEXCOORD0\"],\n")
        genVertices(geoset)
        genParts(geoset)
        builder.append("\t\t}\n")
    }

    private var partCount = 0
    private fun genParts(geoset: Geoset) {
        builder.append("\t\t\t\"parts\": [\n")
        builder.append("\t\t\t{\n")
        partCount = 0
        builder.append("\t\t\t\t\"id\": \"geosetpart_$partCount\",\n")
        builder.append("\t\t\t\t\"type\": \"TRIANGLES\",\n")
        builder.append("\t\t\t\t\"indices\": [\n")
        builder.append("\t\t\t\t\t")
        geoset.faceChunk.faces.forEach({
            builder.append(it.`val`.toString() + ",")
        })
        builder.setLength(builder.length-1)
        builder.append("\t\t\t\t]\n")

        partCount++
        builder.append("\t\t\t}\n")
        builder.append("\t\t\t]\n")
    }


    private fun genVertices(geoset: Geoset) {
        builder.append("\t\t\t\"vertices\": [\n")
        for (i in 0 until geoset.vertexChunk.vertices.size) {
            val vertex = geoset.vertexChunk.vertices[i]
            val normal = geoset.vertexNormalChunk.vertices[i]
            val texCoords = geoset.texCoordSetChunk.matrixGroups[0].texCoords[i]
            builder.append("\t\t\t" + vertex.pos.x.toString() + ", " + vertex.pos.z.toString() + ", " + vertex.pos.y.toString() + ", ")
            builder.append(normal.pos.x.toString() + ", " + normal.pos.z.toString() + ", " + normal.pos.y.toString() + ", ")
            builder.append(texCoords.pos.y.toString() + ", " + texCoords.pos.x.toString() + ",\n")
        }
        builder.append("\t\t\t],\n")
    }

    private fun genId() {
        builder.append("\t\"id\": \"converted mdx\",\n")
    }

    private fun genVersion() {
        builder.append("\t\"version\": [0, 1],\n")
    }

}
