// $Id: SingleCluster.java 55 2011-02-22 21:45:59Z laborg $
/**
 * Copyright 2010 Gerhard Aigner
 * 
 * This file is part of BRISS.
 * 
 * BRISS is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * 
 * BRISS is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * BRISS. If not, see http://www.gnu.org/licenses/.
 */
package at.laborg.briss.utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import at.laborg.briss.model.CropDefinition;

import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.PdfArray;
import com.itextpdf.text.pdf.PdfDictionary;
import com.itextpdf.text.pdf.PdfImportedPage;
import com.itextpdf.text.pdf.PdfName;
import com.itextpdf.text.pdf.PdfNumber;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.PdfSmartCopy;
import com.itextpdf.text.pdf.PdfStamper;
import com.itextpdf.text.pdf.SimpleBookmark;

public class DocumentCropper {

	public static File crop(CropDefinition cropDefinition) throws IOException,
			DocumentException {

		// check if everything is ready
		if (!BrissFileHandling.checkValidStateAndCreate(cropDefinition
				.getDestinationFile()))
			throw new IOException("Destination file not valid");

		// read out necessary meta information
		PdfMetaInformation pdfMetaInformation = new PdfMetaInformation(
				cropDefinition.getSourceFile());

		// first make a copy containing the right amount of pages
		File intermediatePdf = copyToMultiplePages(cropDefinition,
				pdfMetaInformation);

		// now crop all pages according to their ratios
		cropMultipliedFile(cropDefinition, intermediatePdf, pdfMetaInformation);
		return cropDefinition.getDestinationFile();
	}

	private static File copyToMultiplePages(CropDefinition cropDefinition,
			PdfMetaInformation pdfMetaInformation) throws IOException,
			DocumentException {

		PdfReader reader = new PdfReader(cropDefinition.getSourceFile()
				.getAbsolutePath());
		Document document = new Document();

		File resultFile = File.createTempFile("cropped", ".pdf");
		PdfSmartCopy pdfCopy = new PdfSmartCopy(document, new FileOutputStream(
				resultFile));
		document.open();

		for (int pageNumber = 1; pageNumber <= pdfMetaInformation
				.getSourcePageCount(); pageNumber++) {

			PdfImportedPage pdfPage = pdfCopy.getImportedPage(reader,
					pageNumber);

			pdfCopy.addPage(pdfPage);

			List<Float[]> rectangles = cropDefinition
					.getRectanglesForPage(pageNumber);

			for (int j = 1; j < rectangles.size(); j++) {
				pdfCopy.addPage(pdfPage);
			}
		}
		document.close();
		pdfCopy.close();
		reader.close();
		return resultFile;
	}

	private static void cropMultipliedFile(CropDefinition cropDefinition,
			File multipliedDocument, PdfMetaInformation pdfMetaInformation)
			throws FileNotFoundException, DocumentException, IOException {

		PdfReader reader = new PdfReader(multipliedDocument.getAbsolutePath());

		PdfStamper stamper = new PdfStamper(reader, new FileOutputStream(
				cropDefinition.getDestinationFile()));
		stamper.setMoreInfo(pdfMetaInformation.getSourceMetaInfo());

		PdfDictionary pageDict;
		int newPageNumber = 1;

		for (int sourcePageNumber = 1; sourcePageNumber <= pdfMetaInformation
				.getSourcePageCount(); sourcePageNumber++) {

			List<Float[]> rectangleList = cropDefinition
					.getRectanglesForPage(sourcePageNumber);

			// if no crop was selected do nothing
			if (rectangleList.isEmpty()) {
				newPageNumber++;
				continue;
			}

			for (Float[] ratios : rectangleList) {

				pageDict = reader.getPageN(newPageNumber);

				List<Rectangle> boxes = new ArrayList<Rectangle>();
				boxes.add(reader.getBoxSize(newPageNumber, "media"));
				boxes.add(reader.getBoxSize(newPageNumber, "crop"));
				int rotation = reader.getPageRotation(newPageNumber);

				Rectangle scaledBox = RectangleHandler
						.calculateScaledRectangle(boxes, ratios, rotation);

				PdfArray scaleBoxArray = createScaledBoxArray(scaledBox);

				pageDict.put(PdfName.CROPBOX, scaleBoxArray);
				pageDict.put(PdfName.MEDIABOX, scaleBoxArray);
				// increment the pagenumber
				newPageNumber++;
			}
			int[] range = new int[2];
			range[0] = newPageNumber - 1;
			range[1] = pdfMetaInformation.getSourcePageCount()
					+ (newPageNumber - sourcePageNumber);
			SimpleBookmark.shiftPageNumbers(pdfMetaInformation
					.getSourceBookmarks(), rectangleList.size() - 1, range);
		}
		stamper.setOutlines(pdfMetaInformation.getSourceBookmarks());
		stamper.close();
		reader.close();
	}

	private static PdfArray createScaledBoxArray(Rectangle scaledBox) {
		PdfArray scaleBoxArray = new PdfArray();
		scaleBoxArray.add(new PdfNumber(scaledBox.getLeft()));
		scaleBoxArray.add(new PdfNumber(scaledBox.getBottom()));
		scaleBoxArray.add(new PdfNumber(scaledBox.getRight()));
		scaleBoxArray.add(new PdfNumber(scaledBox.getTop()));
		return scaleBoxArray;
	}

	private static class PdfMetaInformation {

		private final int sourcePageCount;
		private final HashMap<String, String> sourceMetaInfo;
		private final List<HashMap<String, Object>> sourceBookmarks;

		public PdfMetaInformation(File source) throws IOException {
			PdfReader reader = new PdfReader(source.getAbsolutePath());
			this.sourcePageCount = reader.getNumberOfPages();
			this.sourceMetaInfo = reader.getInfo();
			this.sourceBookmarks = SimpleBookmark.getBookmark(reader);
			reader.close();

		}

		public int getSourcePageCount() {
			return sourcePageCount;
		}

		public HashMap<String, String> getSourceMetaInfo() {
			return sourceMetaInfo;
		}

		public List<HashMap<String, Object>> getSourceBookmarks() {
			return sourceBookmarks;
		}

	}
}
