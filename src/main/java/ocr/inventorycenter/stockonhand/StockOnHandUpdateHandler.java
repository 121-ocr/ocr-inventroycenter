package ocr.inventorycenter.stockonhand;

import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.ext.mongo.UpdateOptions;
import otocloud.common.ActionURI;
import otocloud.framework.app.function.ActionDescriptor;
import otocloud.framework.app.function.ActionHandlerImpl;
import otocloud.framework.app.function.AppActivityImpl;
import otocloud.framework.core.HandlerDescriptor;
import otocloud.framework.core.OtoCloudBusMessage;

/**
 * 库存中心：现存量-更新
 * 
 * @date 2016年11月20日
 * @author LCL
 */
// 业务活动功能处理器
public class StockOnHandUpdateHandler extends ActionHandlerImpl<JsonObject> {

	public static final String ADDRESS = "update";

	public StockOnHandUpdateHandler(AppActivityImpl appActivity) {
		super(appActivity);
		// TODO Auto-generated constructor stub
	}

	// 此action的入口地址
	@Override
	public String getEventAddress() {
		// TODO Auto-generated method stub
		return ADDRESS;
	}

	// 处理器
	@Override
	public void handle(OtoCloudBusMessage<JsonObject> msg) {

		JsonObject so = msg.body();

		if (stockOnHandNullVal(so) != null && !stockOnHandNullVal(so).equals("")) {
			msg.fail(100, "如下数据不能为空值：" + stockOnHandNullVal(so));
		}

		// 设置存在更新，不存在添加
		MongoClient mongoClient = getCurrentDataSource().getMongoClient();
		mongoClient.updateCollectionWithOptions(getBoFactTableName(this.appActivity.getBizObjectType()),
				getQueryConditon4Update(so), getSetConditon4Update(so), new UpdateOptions(), res -> {
					if (res.succeeded()) {
						JsonObject settingInfo = msg.body();
						settingInfo.put("_id", res.result());
						msg.reply(settingInfo);
					} else {
						Throwable errThrowable = res.cause();
						String errMsgString = errThrowable.getMessage();
						appActivity.getLogger().error(errMsgString, errThrowable);
						msg.fail(100, errMsgString);
					}
				});

	}

	private JsonObject getSetConditon4Update(JsonObject so) {
		JsonObject boData = new JsonObject();
		boData.put("onhandnum", so.getString("onhandnum"));
		JsonObject update = new JsonObject();
		update.put("$set", boData);
		return update;
	}

	private JsonObject getQueryConditon4Update(JsonObject so) {
		JsonObject query = new JsonObject();
		query.put(StockOnHandConstant.sku, so.getString("sku"));
		query.put(StockOnHandConstant.goodaccount, so.getString("goodaccount"));
		query.put(StockOnHandConstant.invbatchcode, so.getString("invbatchcode"));
		query.put(StockOnHandConstant.locationcode, so.getString("locationcode"));
		query.put(StockOnHandConstant.warehousecode, so.getString("warehousecode"));
		return query;
	}

	// 商品SKU+商品租户id+批次号+货位编码+仓库编码+存量
	private String stockOnHandNullVal(JsonObject so) {
		StringBuffer errors = new StringBuffer();

		String sku = so.getString(StockOnHandConstant.sku);
		if (null == sku || sku.equals("")) {
			errors.append("sku");
		}

		String goodaccount = so.getString(StockOnHandConstant.goodaccount);
		if (null == goodaccount || goodaccount.equals("")) {
			errors.append("商品所属货主");
		}

		String warehousecode = so.getString(StockOnHandConstant.warehousecode);
		if (null == warehousecode || warehousecode.equals("")) {
			errors.append("仓库");
		}

		String onhandnum = so.getString(StockOnHandConstant.onhandnum);
		if (null == onhandnum || onhandnum.equals("")) {
			errors.append("现存量");
		}

		return errors.toString();
	}

	/**
	 * 此action的自描述元数据
	 */
	@Override
	public ActionDescriptor getActionDesc() {

		ActionDescriptor actionDescriptor = super.getActionDesc();
		HandlerDescriptor handlerDescriptor = actionDescriptor.getHandlerDescriptor();

		ActionURI uri = new ActionURI("create", HttpMethod.POST);
		handlerDescriptor.setRestApiURI(uri);

		return actionDescriptor;
	}

}
