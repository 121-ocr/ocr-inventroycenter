package ocr.inventorycenter.pharseorder;

/**
 * 采购订单
 * 
 * @author LCL
 *
 */
public class PharseOrderConstant {

	// -----------------组件信息
	public static String ComponentNameConstant = "pharseorder-mgr";
	public static String ComponentBizObjectTypeConstant = "bp_pharseorder";

	// -------------类变量

	// -----------------handler address
	public static String CreateAddressConstant = "create";
	public static String ModifyAddressConstant = "modify";
	public static String QueryAddressConstant = "query";
	public static String RemoveAddressConstant = "remove";
	public static String CreatePharseInvAddressConstant = "batch_createPharseInv";
	
	// -----------------状态变化
	public static String CreatedStatus = "created"; // 创建完毕
	public static String InvStatus = "invwarehouse"; //入库完毕
	
	
	public static final String GOODACCOUNT = "goodaccount";
	public static final String NSNUM = "nsnum";
	public static final String ONHANDNUM = "onhandnum";
	public static final String WAREHOUSECODE = "warehousecode";
	public static final String WAREHOUSES = "warehouses";
	public static final String SKU = "sku";
	public static final String INVBATCHCODE = "invbatchcode";
	public static final String PRODUCT_SKU_CODE = "product_sku_code";
	public static final String GOODS = "goods";
	public static final String WAREHOUSE = "warehouse";
	public static final String EXPDATE = "expdate";
	public static final String BATCH_CODE = "batch_code";
	public static final String INV_DATE = "inv_date";
	public static final String ADDRESS = PharseOrderConstant.CreateAddressConstant;

}
