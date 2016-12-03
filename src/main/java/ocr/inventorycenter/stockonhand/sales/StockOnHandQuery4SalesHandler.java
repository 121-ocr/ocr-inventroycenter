package ocr.inventorycenter.stockonhand.sales;

import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.FindOptions;
import ocr.inventorycenter.stockonhand.StockOnHandConstant;
import ocr.inventorycenter.stockreserved.StockReservedConstant;
import otocloud.common.ActionURI;
import otocloud.framework.app.function.ActionDescriptor;
import otocloud.framework.app.function.ActionHandlerImpl;
import otocloud.framework.app.function.AppActivityImpl;
import otocloud.framework.core.HandlerDescriptor;
import otocloud.framework.core.OtoCloudBusMessage;

/**
 * 库存中心：现存量-查询《返回此SKU的存量（现存量、预留量、可用量》
 * 
 * @date 2016年11月20日
 * @author LCL
 */
// 业务活动功能处理器
public class StockOnHandQuery4SalesHandler extends ActionHandlerImpl<JsonObject> {

	public static final String ADDRESS = "queryatp";

	public StockOnHandQuery4SalesHandler(AppActivityImpl appActivity) {
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

		FindOptions findOptions = new FindOptions();
		// limit
		findOptions.setLimit(2);
		// fields
		JsonObject fields = new JsonObject();
		fields.put("_id", false);
		fields.put(StockOnHandConstant.sku, true);
		fields.put(StockOnHandConstant.goodaccount, true);
		fields.put(StockOnHandConstant.warehousecode, true);
		fields.put(StockOnHandConstant.warehouses, true);
		fields.put(StockOnHandConstant.goods, true);

		findOptions.setFields(fields);

		// sort 1 asc -1 desc
		JsonObject sortFields = new JsonObject();
		sortFields.put(StockOnHandConstant.sku, 1);

		findOptions.setSort(sortFields);

		String sku = so.getString(StockReservedConstant.sku);
		String whcode = so.getString(StockReservedConstant.warehousecode);

		JsonObject queryObj = new JsonObject();
		queryObj.put(StockReservedConstant.sku, sku);
		queryObj.put(StockReservedConstant.warehousecode, whcode);

		JsonObject queryMsg = new JsonObject();
		queryMsg.put("queryObj", queryObj);
		queryMsg.put("resFields", fields);

		this.appActivity.getEventBus().send(this.appActivity.getAppInstContext().getAccount() + "."
				+ "ocr-inventorycenter.stockreserved.querySRNum", queryMsg, onQueryServerdNum -> {
					if (onQueryServerdNum.succeeded()) {

						JsonArray jArray = (JsonArray) onQueryServerdNum.result().body();

						String sernum = jArray.getJsonObject(0).getString(StockReservedConstant.sku);
						so.put("sersku", sernum);

						this.appActivity.getEventBus().send(this.appActivity.getAppInstContext().getAccount() + "."
								+ "ocr-inventorycenter.stockonhand.query", queryMsg, onhandservice -> {

									if (onhandservice.succeeded()) {

										JsonArray jArra2y = (JsonArray) onhandservice.result().body();

										String sernum2 = jArra2y.getJsonObject(0).getString(StockReservedConstant.sku);
										String sersku = so.getString("sersku");

										JsonObject rs = new JsonObject();
										rs.put("sersku", sersku);
										rs.put("sernum2", sernum2);
										msg.reply(rs);
									}

								});

					} else {
					}
				});

	}

	private JsonObject getQueryConditon(JsonObject so) {
		JsonObject query = new JsonObject();
		if (so.containsKey(StockOnHandConstant.sku) && !so.getString(StockOnHandConstant.sku).isEmpty()) {
			query.put(StockOnHandConstant.sku, so.getString(StockOnHandConstant.sku));
		}
		if (so.containsKey(StockOnHandConstant.goodaccount)
				&& !so.getString(StockOnHandConstant.goodaccount).isEmpty()) {
			query.put(StockOnHandConstant.goodaccount, so.getString("goodaccount"));
		}
		// query.put(StockOnHandConstant.sku, so.getString("sku"));
		// query.put(StockOnHandConstant.invbatchcode,
		// so.getString("invbatchcode"));
		// query.put(StockOnHandConstant.locationcode,
		// so.getString("locationcode"));
		query.put(StockOnHandConstant.warehousecode, so.getString("warehousecode"));
		return query;
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
