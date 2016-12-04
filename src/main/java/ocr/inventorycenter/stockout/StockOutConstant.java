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

	// -----------------handler address
	public static String CreateAddressConstant = "create";
	public static String ModifyAddressConstant = "modify";
	public static String QueryAddressConstant = "query";
	public static String RemoveAddressConstant = "remove";
	public static String PickOutAddressConstant = "pickout";// 拣货
	public static String BatchPickOutAddressConstant = "batch_pickout";// 批量拣货
	public static String ShippingOutAddressConstant = "shippingout";// 出库
	
	
	// -----------------状态变化
	public static String CreatedStatus = "created"; //创建完毕
	public static String RemoveStatus = "removed"; //remove完毕
	public static String PickOutedStatus = "pickouted";//拣货完毕
	public static String ShippingStatus = "shippingouted";//出库完毕
	

}
