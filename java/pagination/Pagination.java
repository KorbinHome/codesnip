package com.taobao.mkta.core.common;

import java.util.ArrayList;
import java.util.List;



/**
 * 封装分页信息。 todo:有需要可以再增加一些next，pre方法。
 * 
 * @author gyou
 */
public class Pagination {
    private static final int defaultSize = 40;
    private static final int defaultIndex = 1;
	private int total; // 总记录数
    private int pageIndex; // 当前页, 从1开始计数
	private int pageSize; // 页大小
	private int startRow; // 起始行, 从1开始计数
	private int endRow; // 结束行, 从1开始计数

	public Pagination() {}
	
	public Pagination(Pagination page) {
        this(page.pageIndex, page.pageSize, page.total);
	}

    public Pagination(int pageIndex, int pageSize) {
        if (pageIndex <= 0)
            pageIndex = defaultIndex;
        if (pageSize <= 0)
            pageSize = defaultSize;

        this.pageIndex = pageIndex;
        this.pageSize = pageSize;

        this.startRow = pageSize * (pageIndex - 1) + 1;
        this.endRow = this.startRow + pageSize - 1;

    }

    public Pagination(int pageIndex, int pageSize, int total) {
        this(pageIndex, pageSize);

        this.total = total;
    }

    /**
     * 总页数
     * @return
     */
    public int getPageCount() {
    	int countPage = 0; // 总页数
		if (total % pageSize > 0) {
			countPage = (total / pageSize) + 1;
		} else {
			countPage = total / pageSize;
		}
        return countPage;
    }

    /**
     * @return
     */
    public int getTotal() {
		return total;
	}

    /**
     * @param total
     */
    public void setTotal(int total) {
		this.total = total;
	}

	public int getPageIndex() {
		return pageIndex;
	}

	public void setPageIndex(int pageIndex) {
		this.pageIndex = pageIndex;
	}
	
	public int getPageSize() {
		return pageSize;
	}


	/**
	 * 起始行位置, 从1开始计数
	 * @return
	 */
	public int getStartRow() {
		return startRow;
	}

	public void setStartRow(int startRow) {
		this.startRow = startRow;
	}

	/**
	 * 结束行位置, 从1开始计数
	 * @return
	 */
	public int getEndRow() {
		return endRow;
	}

	public void setEndRow(int endRow) {
		this.endRow = endRow;
	}
	
	public boolean isFirstPage() {
        return this.getPageIndex() == 1;
    }
	
	public int getPreviousPage() {
        int back = this.getPageIndex() - 1;

        if (back <= 0) {
            back = 1;
        }

        return back;
    }
	
	public boolean isLastPage() {
		return this.getPageCount() == this.getPageIndex();
	}

	public int getNextPage() {
		int back = this.getPageIndex() + 1;

		if (back > this.getPageCount()) {
			back = this.getPageCount();
		}

		return back;
	}

	public List<String> getTaobaoSlider() {
    	List<String> l = new ArrayList<String>(10);
    	int leftStart = 1;
    	int leftEnd = 2;
    	int mStart = this.getPageIndex() - 2;
    	int mEnd = this.getPageIndex() + 2;
    	int rStart = this.getPageCount() - 1;
    	int rEnd = this.getPageCount();
    	if (mStart <= leftEnd) {
    		leftStart = 0;
    		leftEnd = 0;
    		mStart = 1;
    	}
    	if (mEnd >= rStart) {
    		rStart = 0;
    		rEnd = 0;
    		mEnd = this.getPageCount();
    	}
    	if (leftEnd > leftStart) {
    		for (int i = leftStart; i <= leftEnd; ++i) {
    			l.add(String.valueOf(i));
    		}
    		l.add("...");
    	}
    	for (int i = mStart; i <= mEnd; ++i) {
    		l.add(String.valueOf(i));
    	}
    	if (rEnd > rStart) {
    		l.add("...");
    		for (int i = rStart; i <= rEnd; ++i) {
    			l.add(String.valueOf(i));
    		}
    	}
    	return l;
    }
	
	
	
	public static void main(String[] args) {
		print(new Pagination(1,10));
		print(new Pagination(2,10));
		
		print(new Pagination(15,10,501));
		print(new Pagination(2,10,100));
		print(new Pagination(10,10,95));
	}

	private static void print(Pagination p) {
		System.out.println("===" + p.getPageIndex() + "," + p.getPageSize() + "," + p.getTotal() + "===");
		System.out.println("start=" + p.getStartRow());
		System.out.println("end=" + p.getEndRow());
		System.out.println("pageCnt=" + p.getPageCount());
		System.out.println("index=" + p.getPageIndex());
		System.out.println(p.getTaobaoSlider());
	}
	
	
}
