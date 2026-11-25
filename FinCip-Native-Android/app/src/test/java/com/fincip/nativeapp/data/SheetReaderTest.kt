package com.fincip.nativeapp.data

import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SheetReaderTest {
    private fun zip(files: Map<String, String>): ByteArray {
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { z -> files.forEach { (n, c) -> z.putNextEntry(ZipEntry(n)); z.write(c.toByteArray()); z.closeEntry() } }
        return out.toByteArray()
    }

    // A statement whose table starts at O15 with a merged title, a date-formatted serial column and shared strings.
    private fun sample(): ByteArray {
        val ss = listOf("Account Statement", "Date", "Narration", "Debit", "Credit", "Balance", "UPI-SWIGGY", "SALARY")
        fun s(i: Int) = """<c r="%s" t="s"><v>$i</v></c>"""
        val rows = StringBuilder()
        rows.append("""<row r="1"><c r="A1" t="s"><v>0</v></c></row>""")
        rows.append("""<row r="15"><c r="O15" t="s"><v>1</v></c><c r="P15" t="s"><v>2</v></c><c r="Q15" t="s"><v>3</v></c><c r="R15" t="s"><v>4</v></c><c r="S15" t="s"><v>5</v></c></row>""")
        // 46327 = 2026-11-01 (1899-12-30 + 46327 days)
        rows.append("""<row r="16"><c r="O16" s="1"><v>46327</v></c><c r="P16" t="s"><v>6</v></c><c r="Q16"><v>250.1</v></c><c r="S16"><v>9749.9</v></c></row>""")
        rows.append("""<row r="17"><c r="O17" s="1"><v>46328</v></c><c r="P17" t="s"><v>7</v></c><c r="R17"><v>50000</v></c><c r="S17"><v>59749.9</v></c></row>""")
        val sheet = """<worksheet><sheetData>$rows</sheetData><mergeCells count="1"><mergeCell ref="A1:C1"/></mergeCells></worksheet>"""
        return zip(mapOf(
            "xl/workbook.xml" to """<workbook xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships"><sheets><sheet name="Statement" sheetId="1" r:id="rId1"/></sheets></workbook>""",
            "xl/_rels/workbook.xml.rels" to """<Relationships><Relationship Id="rId1" Target="worksheets/sheet1.xml"/></Relationships>""",
            "xl/sharedStrings.xml" to "<sst>" + ss.joinToString("") { "<si><t>$it</t></si>" } + "</sst>",
            "xl/styles.xml" to """<styleSheet><cellXfs count="2"><xf numFmtId="0"/><xf numFmtId="14"/></cellXfs></styleSheet>""",
            "xl/worksheets/sheet1.xml" to sheet
        ))
    }

    @Test fun readsDistantCellsDatesAndMerges() {
        val sheets = SheetReader.read(sample())!!
        assertEquals(1, sheets.size); assertEquals("Statement", sheets[0].name)
        val g = sheets[0].grid
        assertEquals("Date", g[14][14]); assertEquals("2026-11-01", g[15][14]); assertEquals("2026-11-02", g[16][14])
        assertEquals("250.1", g[15][16]); assertEquals("9749.9", g[15][18])
        assertEquals("Account Statement", g[0][2]) // merged A1:C1 spreads
    }

    @Test fun endToEndFindsTheTable() {
        val t = SheetTable.find(SheetReader.read(sample())!![0].grid)
        assertNotNull(t); t!!
        assertEquals(listOf("Date", "Description", "Debit", "Credit", "Balance"), t.headers)
        assertEquals(2, t.rows.size); assertEquals("2026-11-01", t.rows[0][0]); assertEquals("50000", t.rows[1][3])
    }

    @Test fun sniffing() {
        assertNull(SheetReader.read("a,b,c\n1,2,3".toByteArray()))
        assertTrue(SheetReader.isZip(sample()))
    }

    @Test fun htmlFallback() {
        val html = "<html><table><tr><th>Date</th><th>Narration</th></tr><tr><td>01/11/2026</td><td>A&nbsp;B</td></tr></table></html>"
        val s = SheetReader.readHtmlTable(html)!!
        assertEquals("A B", s.grid[1][1]); assertNull(SheetReader.readHtmlTable("plain text"))
    }
}
