package ocr.inventorycenter.stockonhand;

/**
 * 现存量 常量类
 * 
 * @author LCL
 *
 */
public class StockOnHandConstant {
	// -----------------冗余信息
	public static String bo_id = "bo_id";
	public static String account = "account";
	public static String locations = "locations";
	public static String warehouses = "warehouses";
	public static String goods = "goods";

	// -----------------维度信息

	public static String sku = "sku";
	public static String goodaccount = "goodaccount";
	public static String invbatchcode = "invbatchcode";
	public static String locationcode = "locationcode";
	public static String warehousecode = "warehousecode";
	public static String onhandnum = "onhandnum";

	// -----------------组件信息
	public static String ComponentNameConstant = "stockonhand-mgr";
	public static String ComponentBizObjectTypeConstant = "bs_stockonhand";

	// -----------------handler address
	public static String CreateAddressConstant = "create";
	public static String ModifyAddressConstant = "modify";
	public static String QueryAddressConstant = "query";
	public static String RemoveAddressConstant = "remove";
	public static String BatchCreateAddressConstant = "batchcreate";

}
