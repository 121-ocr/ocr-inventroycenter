package ocr.inventorycenter.stockonhand;

import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import otocloud.common.ActionURI;
import otocloud.framework.app.function.ActionDescriptor;
import otocloud.framework.app.function.ActionHandlerImpl;
import otocloud.framework.app.function.AppActivityImpl;
import otocloud.framework.core.HandlerDescriptor;
import otocloud.framework.core.OtoCloudBusMessage;

/**
 * 库存中心：现存量-创建
 * 
 * @date 2016年11月20日
 * @author LCL
 */
// 业务活动功能处理器
public class StockOnHandCreationHandler extends ActionHandlerImpl<JsonObject> {

	public static final String ADDRESS = "create";

	public StockOnHandCreationHandler(AppActivityImpl appActivity) {
		super(appActivity);
	}

	// 此action的入口地址
	@Override
	public String getEventAddress() {
		return ADDRESS;
	}

	// 处理器
	@Override
	public void handle(OtoCloudBusMessage<JsonObject> msg) {
		JsonObject so = msg.body();

		if (stockOnHandNullVal(so) != null && !stockOnHandNullVal(so).equals("")) {
			msg.fail(100, "如下数据不能为空值："+stockOnHandNullVal(so));
		}

		// 现存量维度=商品SKU+商品租户id+批次号+货位编码+仓库编码+存量+冗余字段 {主键+租户id+货位集合+仓库集合+商品集合}
		JsonObject settingInfo = getParams(msg, so);
		appActivity.getAppDatasource().getMongoClient().save(appActivity.getDBTableName(appActivity.getBizObjectType()),
				settingInfo, result -> {
					if (result.succeeded()) {
						settingInfo.put("_id", result.result());
						msg.reply(settingInfo);
					} else {
						Throwable errThrowable = result.cause();
						String errMsgString = errThrowable.getMessage();
						appActivity.getLogger().error(errMsgString, errThrowable);
						msg.fail(100, errMsgString);
					}
				});

	}

	private JsonObject getParams(OtoCloudBusMessage<JsonObject> msg, JsonObject so) {
		JsonObject settingInfo = msg.body();
		//settingInfo.put(StockOnHandConstant.bo_id, "");
		settingInfo.put(StockOnHandConstant.account, this.appActivity.getAppInstContext().getAccount());
		settingInfo.put(StockOnHandConstant.locations, so.getValue("locations"));
		settingInfo.put(StockOnHandConstant.warehouses, so.getValue("warehouses"));
		settingInfo.put(StockOnHandConstant.goods, so.getValue("goods"));
		settingInfo.put(StockOnHandConstant.sku, so.getString("sku"));
		settingInfo.put(StockOnHandConstant.goodaccount, so.getString("goodaccount"));
		settingInfo.put(StockOnHandConstant.invbatchcode, so.getString("invbatchcode"));
		settingInfo.put(StockOnHandConstant.locationcode, so.getString("locationcode"));
		settingInfo.put(StockOnHandConstant.warehousecode, so.getString("warehousecode"));
		settingInfo.put(StockOnHandConstant.onhandnum, so.getInteger("onhandnum"));
		return settingInfo;
	}

	private String stockOnHandNullVal(JsonObject so) {
		StringBuffer errors = new StringBuffer();
		
		Object locations = so.getValue(StockOnHandConstant.locations);

		if (null == locations || locations.equals("")) {
			errors.append("货位");
		}

		Object warehouses = so.getValue(StockOnHandConstant.warehouses);

		if (null == warehouses || warehouses.equals("")) {
			errors.append("仓库");
		}

		String sku = so.getString(StockOnHandConstant.sku);
		if (null == sku || sku.equals("")) {
			errors.append("sku");
		}
		
		String goodaccount = so.getString(StockOnHandConstant.goodaccount); 
		if (null == goodaccount || goodaccount.equals("")) {
			errors.append("商品所属货主");
		}
		
		Object goods = so.getValue(StockOnHandConstant.goods); 

		if (null == goods || goods.equals("")) {
			errors.append("商品信息");
		}
		
		if(!so.containsKey(StockOnHandConstant.onhandnum)){
			errors.append("现存量");
		}

/*		String num = so.getString(StockOnHandConstant.onhandnum);
		if (null == num || num.equals("")) {
			errors.append("现存量");
		}*/

		return errors.toString();
	}

	/**
	 * 此action的自描述元数据
	 */
	@Override
	public ActionDescriptor getActionDesc() {

		ActionDescriptor actionDescriptor = super.getActionDesc();
		HandlerDescriptor handlerDescriptor = actionDescriptor.getHandlerDescriptor();

		ActionURI uri = new ActionURI(ADDRESS, HttpMethod.POST);
		handlerDescriptor.setRestApiURI(uri);

		return actionDescriptor;
	}

}
