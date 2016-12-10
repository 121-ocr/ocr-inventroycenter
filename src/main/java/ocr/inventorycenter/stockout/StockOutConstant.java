package ocr.inventorycenter.stockout;

/**
 * 现存量 常量类
 * 
 * @author LCL
 *
 */
public class StockOutConstant {

	
	// -----------------组件信息
	public static String ComponentNameConstant = "stockout-mgr";
	public static String ComponentBizObjectTypeConstant = "bp_stockout";
	
	
	// -------------类变量
	public static final String batch_code = "batch_code";
	public static final String detail_code = "detail_code";
	public static final String goods = "goods";
	public static final String product_sku_code = "product_sku_code";;
	public static final String quantity_should = "quantity_should";;
	public static final String warehouse = "warehouse";;
	
	// -----------------handler address
	public static String CreateAddressConstant = "create";
	public static String ModifyAddressConstant = "modify";
	public static String QueryAddressConstant = "query";
	public static String RemoveAddressConstant = "remove";
	public static String PickOutAddressConstant = "pickout";// 拣货
	public static String BatchCreateAddressConstant = "batch_create";// 批量创建拣货单
	public static String BatchPickOutAddressTestConstant = "test";// 批量拣货
	public static String ONShippingOutAddressConstant = "onshipping";// 出库
	public static String ShippingOutAddressConstant = "shippingout";// 出库
	
	
	// -----------------状态变化
	public static String CreatedStatus = "created"; //创建完毕
	public static String ONPickingStatus = "onpicking";//拣货中
	public static String PickOutedStatus = "pickouted";//拣货完毕
	public static String ShippingStatus = "shippingouted";//出库完毕
	
	

}
