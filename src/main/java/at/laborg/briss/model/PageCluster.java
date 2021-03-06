// $Id$
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
package at.laborg.briss.model;

import java.util.ArrayList;
import java.util.List;

public class PageCluster implements Comparable<PageCluster> {

	private static final int MERGE_VARIABILITY = 20;
	private static final int MAX_MERGE_PAGES = 15;

	private List<Integer> pagesToMerge;
	private final List<Integer> allPages;
	private final List<Float[]> cropRatiosList = new ArrayList<Float[]>();

	private boolean excluded = false;

	private ClusterImageData imageData;

	private final boolean evenPage;
	private final int pageWidth;
	private final int pageHeight;

	public PageCluster(final boolean isEvenPage, final int pageWidth, final int pageHeight,
			final boolean excluded, final int pageNumber) {
		super();
		this.pageWidth = pageWidth;
		this.pageHeight = pageHeight;
		this.evenPage = isEvenPage;
		this.excluded = excluded;
		this.pagesToMerge = new ArrayList<Integer>();
		this.allPages = new ArrayList<Integer>();
		this.allPages.add(pageNumber);
	}

	public final ClusterImageData getImageData() {
		if (imageData == null) {
			imageData = new ClusterImageData(pageWidth, pageHeight,
					pagesToMerge.size());
		}
		return imageData;
	}

	/**
	 * Returns the ratio to crop the page.
	 * 
	 * returns the ratio to crop the page x1,y1,x2,y2, origin = bottom left x1:
	 * from left edge to left edge of crop rectange y1: from lower edge to lower
	 * edge of crop rectange x2: from right edge to right edge of crop rectange
	 * y2: from top edge to top edge of crop rectange
	 * 
	 * @return
	 */
	public final List<Float[]> getRatiosList() {
		return cropRatiosList;
	}

	public final void setRatiosList(final List<Float[]> ratiosList) {
		clearRatios();
		for (final Float[] ratios : ratiosList)
			addRatios (ratios);
	}

	public final void clearRatios() {
		cropRatiosList.clear();
	}

	public final void addRatios(final Float[] ratios) {
		// check if already in
		if (!cropRatiosList.contains(ratios)) {
			cropRatiosList.add(ratios);
		}
	}

	public final boolean isClusterNearlyEqual(final PageCluster other, boolean evenOddDifferent) {
		if (evenOddDifferent && evenPage != other.evenPage)
			return false;
		if (excluded || other.excluded)
			return false;
		if (getRoundedPageHeight() != other.getRoundedPageHeight())
			return false;
		if (getRoundedPageWidth() != other.getRoundedPageWidth())
			return false;
		return true;
	}

	public final void mergeClusters(final PageCluster other) {
		allPages.addAll(other.getAllPages());
	}

	public final boolean isEvenPage() {
		return evenPage;
	}

	public final int getRoundedPageHeight() {
		int tmp = pageHeight / MERGE_VARIABILITY;
		return tmp * MERGE_VARIABILITY;
	}

	public final int getRoundedPageWidth() {
		int tmp = pageWidth / MERGE_VARIABILITY;
		return tmp * MERGE_VARIABILITY;
	}

	public final void choosePagesToMerge() {
		if (allPages.size() < MAX_MERGE_PAGES) {
			// use all pages
			pagesToMerge = allPages;
		} else {
			// use an equal distribution
			float stepWidth = (float) allPages.size() / MAX_MERGE_PAGES;
			float totalStepped = 0;
			for (int i = 0; i < MAX_MERGE_PAGES; i++) {
				pagesToMerge.add(allPages.get(new Double(Math
						.floor(totalStepped)).intValue()));
				totalStepped += stepWidth;
			}
		}
	}

	public final List<Integer> getAllPages() {
		return allPages;
	}

	public final List<Integer> getPagesToMerge() {
		return pagesToMerge;
	}

	public final int compareTo(final PageCluster that) {
		return this.getFirstPage() - that.getFirstPage();
	}

	private int getFirstPage() {
		int small = Integer.MAX_VALUE;
		for (Integer tmp : allPages) {
			if (tmp < small) {
				small = tmp;
			}
		}
		return small;
	}

}
