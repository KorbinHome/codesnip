package com.taobao.mkta.web.module.screen;

import java.text.MessageFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import org.apache.commons.lang.time.DateUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.alibaba.citrus.turbine.Context;
import com.alibaba.citrus.turbine.TurbineRunData;
import com.alibaba.citrus.turbine.dataresolver.Param;
import com.alibaba.common.lang.StringUtil;
import com.taobao.common.searchengine.SearchResult;
import com.taobao.forest.service.StdCategoryServices;
import com.taobao.ipda.core.domain.AuctionSummaryDO;
import com.taobao.ipda.core.service.CatSummaryManager;
import com.taobao.ipda.mysearch.SummarySearchQuery;
import com.taobao.ipda.mysearch.VSearchSchema;
import com.taobao.ipda.search.SearchProxy;
import com.taobao.ipda.web.ao.ItemCenterAO;
import com.taobao.ipda.web.po.GoodsPO;
import com.taobao.mkta.core.common.Pagination;
import com.taobao.mkta.web.po.AuctionQueryPO;
import com.taobao.vsearch.client.VSearchQuery.ORDER;

/**
 * @author guoyou
 * 促销参考的商品列表
 */
public class ActivityRefer extends MktaScreenBase {
	
	private Log logger = LogFactory.getLog(this.getClass());
	
	@Resource
	private SearchProxy searchProxy;
	
	@Resource
	private StdCategoryServices stdCategoryServices;
	
	@Resource
	private CatSummaryManager catSummaryManager;

	@Resource
	private ItemCenterAO itemCenterAO;
	
	/**
	 * 每页记录数
	 */
	private static final int PAGE_SIZE=10;
	
	
	public void execute(Context context, TurbineRunData runData, 
            @Param(name="timeBegin")String timeBegin,
            @Param(name="timeEnd")String timeEnd,
            @Param(name="title")String title,
            @Param(name="code")String code,
            @Param(name="categoryId")Long category,
            @Param(name="cPage", defaultValue="1")Integer pageNO,
            @Param(name="orderSalesAmount")Integer orderSalesAmount,
            @Param(name="orderSalesQuantity")Integer orderSalesQuantity,
            @Param(name="orderConversionRate")Integer orderConversionRate,
            @Param(name="orderUv")Integer orderUv
            
			) {
		
		
		AuctionQueryPO query = createQueryPO(
				timeBegin,
				timeEnd,
				title,
				code,
				category,
				orderSalesAmount,
				orderSalesQuantity,
				orderConversionRate,
				orderUv
				);
		
		
		if (query.getLatestDays() == null) {
			query.setLatestDays(7);
		}
		
		context.put("query", query);
		
		Map<Long, String> catNameMap = catSummaryManager.getCatNameMap(getUserId(), null);
		context.put("catPathMap", itemCenterAO.getCatPathMap(catNameMap));
		
		Pagination page = new Pagination(pageNO, PAGE_SIZE);
		
		SummarySearchQuery searchQuery=createAuctionSummaryQuery(query, page);
		
		SearchResult sr = searchProxy.auctionSummarySearch(searchQuery);
		
		page.setTotal(sr.getAllNum());
		
		if(sr.isSuccess() && sr.getResultList().size() > 0) {
			List<Long> catIdList = new ArrayList<Long>();
			List<Long> itemIds = new ArrayList<Long>();
			for (Object obj : sr.getResultList()) {
				AuctionSummaryDO itemDO = (AuctionSummaryDO)obj;
				catIdList.add(itemDO.getCatId());
				itemIds.add(itemDO.getAuctionId());
			}
			
			@SuppressWarnings("unchecked")
			List<GoodsPO> goodsResultList = converPO(getDateType(query.getLatestDays()), 
					sr.getResultList());
			
			context.put("page", page);
			context.put("catNameMap", getCatNameMap(catIdList));
			context.put("goodsResultList", goodsResultList);
			context.put("itemImageMap", itemCenterAO.getItemMainImages(itemIds));
		}
		
	}
	
	private Map<Long, String> getCatNameMap(List<Long> catIdList) {
		return catSummaryManager.getCatNameMap(getUserId(), catIdList);
	}
	
	public List<GoodsPO> converPO(int dateType, List<AuctionSummaryDO> auctionList) {
		List<GoodsPO> goodsPOs = new ArrayList<GoodsPO>();
		
		for(AuctionSummaryDO auctionSummaryDO : auctionList) {
			GoodsPO goodsPO = new GoodsPO(dateType, auctionSummaryDO);
			goodsPOs.add(goodsPO);
		}
		
		return goodsPOs;
	}
		

	private SummarySearchQuery createAuctionSummaryQuery(AuctionQueryPO query, Pagination page) {
		SummarySearchQuery query2 = new SummarySearchQuery();
		query2.addFilterField(VSearchSchema.seller_id, getUserId()+"");
		query2.addFilterField(VSearchSchema.auction_title, query.getTitle());
		query2.addFilterField(VSearchSchema.outer_id, query.getCode());
		query2.addFilterField(VSearchSchema.cat_id, query.getCategoryId()+"");
		query2.setStart(page.getStartRow()-1);
		query2.setRows(page.getPageSize());
		
		setOrderParam(query2, getDateType(query.getLatestDays()), 
				query.getOrderSalesAmount(),
				query.getOrderSalesQuantity(),
				query.getOrderUv(),
				query.getOrderConversionRate(),
				null
				);
		
		return query2;
	}
	
	
	private int getDateType(int latestDays) {
		int dateType=0;
		if (latestDays==1) {
			dateType=0;
		}else if (latestDays==7) {
			dateType=1;
		}else if (latestDays==30) {
			dateType=2;
		}
		return dateType;
	}
	
	private void setOrderParam(SummarySearchQuery query2, int dateType, final Integer orderAmtAlipay, 
			final Integer orderQtyAlipay, final Integer orderUv, final Integer orderConversionRate,
			final Integer orderQuantitySellDay) {
		//销售额排序
		String orderBy;
		String fP = "{0}{1}";
		String x = Float.MIN_NORMAL+"";
		String fpRate = "div(sub({0}{1},{0}o{1}),add({0}o{1},"+x +"))";
		if(orderAmtAlipay != null) {
			String prefix = "amt_alipay_";
			setQuery(query2, dateType, orderAmtAlipay, fP, fpRate, prefix);
			return;
		}
		// 销售量排序
		if (orderQtyAlipay != null) {
			String prefix = "qty_alipay_";
			setQuery(query2, dateType, orderQtyAlipay, fP, fpRate, prefix);
			return;
		}
		// 访问量排序
		if (orderUv != null) {
			String prefix = "uv_";
			setQuery(query2, dateType, orderUv, fP, fpRate, prefix);
			return;
		}
		// 可售天数排序
		if (orderQuantitySellDay != null) {
			query2.setSortField("sub(product(div(sum(quantity,on_order_inventory),qty_alipay_d7),7),1)");
			query2.setSortOrder(orderQuantitySellDay == 1 ? ORDER.desc : ORDER.asc);
			return;
		}
		// 转化率排序
		if (orderConversionRate != null) {
			fP = "div({0}{2},add({1}{2},"+x+"))";
			fpRate = "div(" +
							"sub( " +
								"div({0}{2},add({1}{2},"+x+"))" + ", " +
								"div({0}o{2},add({1}o{2},"+x+"))" 
								+")" +
							" , " +
							"add(" +
								"div({0}o{2},add({1}o{2},"+x+"))" + "," +
								 x 
								 +")" +
						")";
			switch (orderConversionRate) {
			case 1:
				orderBy = MessageFormat.format(fP, "user_cnt_alipay_", "uv_", getOrdersuffix(dateType));
				query2.setSortField(orderBy);
				query2.setSortOrder(ORDER.asc);
				return;
			case 2:
				orderBy = MessageFormat.format(fP, "user_cnt_alipay_", "uv_", getOrdersuffix(dateType));
				query2.setSortField(orderBy);
				query2.setSortOrder(ORDER.desc);
				return;
			case 3:
				orderBy = MessageFormat.format(fpRate, "user_cnt_alipay_", "uv_", getOrdersuffix(dateType));
				query2.setSortField(orderBy);
				query2.setSortOrder(ORDER.asc);
				return;
			case 4:
				orderBy = MessageFormat.format(fpRate, "user_cnt_alipay_", "uv_", getOrdersuffix(dateType));
				query2.setSortField(orderBy);
				query2.setSortOrder(ORDER.desc);
				return;
			}
		}
		
		query2.setSortField(VSearchSchema.qty_alipay_d7);
		query2.setSortOrder(ORDER.desc);
	}
	
	
	protected void setQuery(SummarySearchQuery query2, int dateType,
			final Integer orderAmtAlipay, String fP, String fpRate,
			String prefix) {
		String orderBy;
		switch (orderAmtAlipay) {
		case 1:
			orderBy = MessageFormat.format(fP, prefix, getOrdersuffix(dateType));
			query2.setSortField(orderBy);
			query2.setSortOrder(ORDER.asc);
			return;
		case 2:
			orderBy = MessageFormat.format(fP, prefix, getOrdersuffix(dateType));
			query2.setSortField(orderBy);
			query2.setSortOrder(ORDER.desc);
			return;
		case 3:
			orderBy = MessageFormat.format(fpRate, prefix, getOrdersuffix(dateType));
			query2.setSortField(orderBy);
			query2.setSortOrder(ORDER.asc);
			return;
		case 4:
			orderBy = MessageFormat.format(fpRate, prefix, getOrdersuffix(dateType));
			query2.setSortField(orderBy);
			query2.setSortOrder(ORDER.desc);
			return;
		}
	}
	



	private String getOrdersuffix(int dateType) {
		switch(dateType){
		case 0:
			return "d1";
		case 1:
			return "d7";
		case 2:
			return "d30";
		case 3:
			return "w1";
		case 4:
			return "m1";
		}
		return "d1";
	}
	
	
	
	private AuctionQueryPO createQueryPO(
			String timeBegin, String timeEnd,
			String title, String code, Long category,
			Integer orderSalesAmount, Integer orderSalesQuantity, 
			Integer orderConversionRate, Integer orderUv
			) {
		
		AuctionQueryPO query = new AuctionQueryPO();
		
		try {
			SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/M/dd");
			Date begin = StringUtil.isNotBlank(timeBegin) ? dateFormat.parse(timeBegin) : null;
			Date end = StringUtil.isNotBlank(timeEnd) ? dateFormat.parse(timeEnd) : null ;
			
			query.setTimeBegin(begin);
			query.setTimeEnd(end);
			
			//如果end是昨天，则表示近n天，填充 latestDays属性。
			if(end != null 
					&& begin != null 
					&& DateUtils.isSameDay( DateUtils.addDays(end, 1), new Date() ) ) {
				int days = (int) ( (end.getTime() - begin.getTime())/ (24*3600*1000L) ) ;
				query.setLatestDays(days+1);
			}
		} catch (ParseException e) {
			logger.error("查询条件中的日期格式错误！",e);
		}
		
		
		query.setTitle(title);
		query.setCode(code);
		query.setCategoryId(category);
		
		query.setOrderSalesAmount(orderSalesAmount);
		query.setOrderSalesQuantity(orderSalesQuantity);
		query.setOrderConversionRate(orderConversionRate);
		query.setOrderUv(orderUv);	
		
		return query;
	}
	
}
