package com.patho.filewatcher

import com.google.zxing.*
import com.google.zxing.client.j2se.BufferedImageLuminanceSource
import com.google.zxing.common.HybridBinarizer
import org.apache.commons.io.FileUtils
import org.apache.pdfbox.cos.COSDictionary
import org.apache.pdfbox.multipdf.PDFCloneUtility
import org.apache.pdfbox.multipdf.Splitter
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.rendering.ImageType
import org.apache.pdfbox.rendering.PDFRenderer
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.Resource
import org.springframework.core.io.ResourceLoader
import org.springframework.core.io.support.ResourcePatternUtils
import org.springframework.stereotype.Service
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.nio.charset.Charset
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.*


@Service
open class WatcherService @Autowired constructor(
        private val resourceLoader: ResourceLoader,
        private val mailService: MailService,
        private val config: Config) {

    private val logger: Logger = LoggerFactory.getLogger(this.javaClass)

    fun getFolder(path: String): String {
        val year = LocalDate.now().year.toString()
        return path.replace("{year}", year)
    }

    fun getResource(path: String): Resource {
        return resourceLoader.getResource(getFolder(path))
    }

    open fun getFilesOFWatchDir(dir: String): List<Resource> {
        return try {
            val files = ResourcePatternUtils.getResourcePatternResolver(resourceLoader).getResources(getFolder(dir))
            files.toList()
        } catch (e: IOException) {
            e.printStackTrace()
            listOf()
        }
    }

    var keepOriginalFiles = false

    open fun processPDFs(files: List<File>, saveProcessedFiles: Boolean, processedFilesDir: String, moveOriginalFiles: Boolean, originalFilesDir: String): MutableList<PDFPageResult> {

        val resultList = mutableListOf<PDFPageResult>()

        for (file in files) {
            logger.debug("Reading file ${file.path}")

            val fileResult = mutableListOf<PDFPageResult>()

            val document = PDDocument.load(file);
            val splitter = Splitter()
            val pages = splitter.split(document)

            val iter = pages.listIterator()

            var pdfPageResult = PDFPageResult()
            var i = 1
            while (iter.hasNext()) {
                val page = iter.next()
                logger.debug("Page $i ## Start with page $i")

                pdfPageResult.pages.add(page)

                val renderer = PDFRenderer(page)
                val im = renderer.renderImageWithDPI(0, 500F, ImageType.GRAY)


                for (border in config.splitImages) {
                    val suImage = getSubImage(im, border)
                    val result = readCode(suImage)

                    if (result != null) {
                        pdfPageResult.tags.put(border.jsonTag, result)
                        logger.debug("Page $i ## Result for json Tag: ${border.jsonTag} = $result; ")
                    } else
                        logger.error("Page $i ## No Result for json Tag ${border.jsonTag} found!")
                }
                //ImageIO.write(im, "PNG", File(getFolder(config.targetDir).replace("file:", "") + "/" + time + "_" + i + ".png"))

                // document of its own
                if (pdfPageResult.tags.size >= config.appendToPrevPageDetectionThreshold) {
                    if (fileResult.isNotEmpty()) {
                        logger.debug("Page $i ## Saved!!")
                        if (saveProcessedFiles)
                            saveNewPDF(fileResult.last(), processedFilesDir)
                        else
                            fileResult.last().pages.forEach { it.close() }
                    }
                    logger.debug("Page $i ## Is new Document")
                    fileResult.add(pdfPageResult)
                } else {
                    // back of a document
                    if (fileResult.size > 0) {
                        logger.debug("Page $i ## Is back page of old document")
                        val last = fileResult.last()
                        last.pages.addAll(pdfPageResult.pages)
                    } else {
                        logger.error("Page $i ## Is back page of old document, error no old document found!")
                        fileResult.add(pdfPageResult)
                    }
                }

                pdfPageResult = PDFPageResult()
                i++
                logger.debug("Page $i ## End with page $i")
                logger.debug("-------------------")
            }

            if (fileResult.isNotEmpty()) {
                logger.debug("Page $i ## Saved!!")
                if (saveProcessedFiles)
                    saveNewPDF(fileResult.last(), processedFilesDir)
                else
                    fileResult.last().pages.forEach { it.close() }
            }

            resultList.addAll(fileResult)

            document.close()

            if (moveOriginalFiles)
                FileUtils.moveFileToDirectory(file, getResource(originalFilesDir).file, true)
        }

        return resultList
    }

    private fun saveNewPDF(pdf: PDFPageResult, processedFilesDir: String) {
        val document = PDDocument()

        pdf.pages.forEach { document.addPage(it.getPage(0)) }
        val targetFile = if (validate(pdf)) {
            val otherFiles = getFilesOFWatchDir("$processedFilesDir/*${pdf.target}*")
            val name = "${SimpleDateFormat("yyyy-MM-dd--HH-mm-ss").format(Date())}_${pdf.target}" + (if (otherFiles.isNotEmpty()) "_N_" + otherFiles.size else "") + ".pdf"
            pdf.valid = true
            "$processedFilesDir/$name"
        } else {
            val name = "${SimpleDateFormat("yyyy-MM-dd--HH-mm-ss").format(Date())}_${pdf.target}.pdf"
            pdf.valid = false
            "$processedFilesDir/$name"
        }

        val byteArrayOutputStream = ByteArrayOutputStream()
        document.save(byteArrayOutputStream)
        document.close()
        pdf.pdfAsByts = byteArrayOutputStream.toByteArray()

        try {
            FileUtils.writeByteArrayToFile(getResource(targetFile).file, pdf.pdfAsByts)
            val targetJson = targetFile.replace(".pdf", ".json");
            val subFolderJson = targetJson.substringBeforeLast("/") +"/json/"+targetJson.substringAfterLast("/")
            FileUtils.writeStringToFile(getResource(subFolderJson).file, pdf.getJson(), Charset.forName("UTF8"))
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            pdf.pages.forEach { it.close() }
        }
    }

    private fun validate(pdf: PDFPageResult): Boolean {
        val caseNumber = pdf.tags["caseNumber"]
        val qrNumber = pdf.tags["qr"]

        // getting case number from barcode
        if (caseNumber.isNullOrEmpty() && caseNumber?.matches(Regex("[0-9]{6,}")) == true) {
            pdf.target = caseNumber
            return true
        } else {
            // failover, getting number from qr code taskNumber;caseNumber;documentNumber
            if (qrNumber != null) {
                val arr = qrNumber.split(";")
                if (arr.size == 3 && arr[1].matches(Regex("[0-9]{6,}"))) {
                    pdf.target = arr[1]
                    return true
                }
            }
        }

        logger.error("! No TaskNumber Found !")
        pdf.target = "_Fehler"
        return false
    }

    private fun clonePages(pdf: PDDocument): PDPage {
        val pageBase = PDFCloneUtility(pdf).cloneForNewDocument(pdf.pages[0].getCOSObject())
        return PDPage(COSDictionary(pageBase as COSDictionary))
    }

    private fun getSubImage(im: BufferedImage, border: Config.SubImageBorders): BufferedImage {
        val x = border.x
        val y = border.y
        val width = if (border.width == -1) im.width - x else border.width
        val height = if (border.height == -1) im.height - y else border.height
        return im.getSubimage(x, y, width, height)
    }

    open fun readCode(image: BufferedImage): String? {

        val hints = hashMapOf<DecodeHintType, Any>();
        hints[DecodeHintType.TRY_HARDER] = true as Any;

        val source: LuminanceSource = BufferedImageLuminanceSource(image)
        val bitmap = BinaryBitmap(HybridBinarizer(source))
        val reader = MultiFormatReader()

        return try {
            val result = reader.decode(bitmap, hints)
            result.text
        } catch (e: NotFoundException) {
            null
        }
    }
}