package ocr.inventorycenter.stockreserved;

/**
 * 现存量 常量类
 * 
 * @author LCL
 *
 */
public class StockReservedConstant {

	// 字段常量
	public static String bo_id = "bo_id";
	public static String account = "account";
	public static String locations = "locations";
	public static String warehouses = "warehouses";
	public static String goods = "goods";
	public static String sku = "sku";
	
	
	
	
	public static String invbatchcode = "invbatchcode";

	public static String locationcode = "locationcode";
	public static String warehousecode = "warehousecode";
	public static String reserverdnum = "reserverdnum";

	public static String pickoutid = "pickoutid";
	public static String pickoutnum = "pickoutnum";
	public static String goodaccount = "goodaccount";
	
	public static String quantity_should = "quantity_should";
	public static String batch_code = "batch_code";
	
	
	// -----------------组件信息
	public static String ComponentNameConstant = "stockreserved";
	public static String ComponentBizObjectTypeConstant = "bs_stockreserved";

	// -----------------handler address
	public static String ReservedAddressConstant = "reserved";
	public static String ModifyAddressConstant = "modify";
	public static String QueryAddressConstant = "query";
	public static String RemoveAddressConstant = "remove";
	
	// -----------------状态变化
	public static String CreatedStatus = "created"; //创建完毕
	public static String RemoveStatus = "removed"; //remove完毕

}
