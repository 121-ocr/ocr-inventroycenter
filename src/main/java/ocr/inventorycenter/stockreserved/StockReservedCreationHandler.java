package ocr.inventorycenter.stockreserved;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import otocloud.common.ActionURI;
import otocloud.framework.app.function.ActionDescriptor;
import otocloud.framework.app.function.ActionHandlerImpl;
import otocloud.framework.app.function.AppActivityImpl;
import otocloud.framework.core.HandlerDescriptor;
import otocloud.framework.core.OtoCloudBusMessage;

/**
 * 库存中心：预留 --创建预留
 * 
 * @date 2016年11月20日
 * @author LCL
 */
// 业务活动功能处理器
public class StockReservedCreationHandler extends ActionHandlerImpl<JsonObject> {

	public static final String ADDRESS = "reserved";

	public static final String ONHAND_REGISTER = "ocr-inventorycenter.stocknohand.query";

	public StockReservedCreationHandler(AppActivityImpl appActivity) {
		super(appActivity);
	}

	// 此action的入口地址
	@Override
	public String getEventAddress() {
		// TODO Auto-generated method stub
		return ADDRESS;
	}

	// 处理器
	// 步骤1、根据传入参数{商品SKU+仓库编码}获取对应现存量
	// 步骤2、校验传入参数{拣货单.拣货数量} <= {步骤1.现存量- SUM{预留表{相同 商品SKU+仓库编码}}}。
	// 步骤3、如果步骤2 ok 则成功预留。否则预留拣货失败（目前整单预留，不进行部分预留）
	// -------------------
	@Override
	public void handle(OtoCloudBusMessage<JsonObject> msg) {
		JsonObject so = msg.body();

		if (stockOnHandNullVal(so) != null && !stockOnHandNullVal(so).equals("")) {
			msg.fail(100, stockOnHandNullVal(so));
		}
		// 步骤1, 获取现存量
		this.appActivity.getEventBus().send(getOnHandAddress(), getOnHandParam(so), onhandservice -> {
			if (onhandservice.succeeded()) {
				JsonObject onhands = (JsonObject) onhandservice.result().body();
				Double onhandnum = onhands.getDouble("onhandnum");
				// 步骤2、校验数量是否可以预留
				isCanReserved(onhandnum, so, AsyncResult -> {
					if (AsyncResult.succeeded()) {
						// 步骤3、预留此拣货单id+sku+数量
						executeReserved(so, AsyncResultsub -> {
							if (AsyncResultsub.succeeded()) {
								JsonArray srvCfg = new JsonArray("预留成功");
								msg.reply(srvCfg);
							} else {
								Throwable errThrowable = AsyncResultsub.cause();
								String errMsgString = errThrowable.getMessage();
								appActivity.getLogger().error(errMsgString, errThrowable);
								msg.fail(100, errMsgString);
							}
						});
						msg.reply("ok");
					} else {
						Throwable err = AsyncResult.cause();
						err.printStackTrace();
						msg.fail(100, err.getCause().getMessage());
					}
				});

				msg.reply("ok");
			} else {
				Throwable err = onhandservice.cause();
				String errMsg = err.getMessage();
				componentImpl.getLogger().error(errMsg, err);
				msg.fail(100, errMsg);
			}

		});

	}

	private JsonObject getOnHandParam(JsonObject so) {
		JsonObject params = new JsonObject();
		params.put("sku", so.getString("sku"));
		params.put("warehousecode", so.getString("warehousecode"));
		return params;
	}

	private String getOnHandAddress() {
		String authSrvName = componentImpl.getDependencies().getJsonObject("ocr-inventorycenter")
				.getString("service_name", "");
		String address = authSrvName + "." + ONHAND_REGISTER;
		return address;
	}

	private void executeReserved(JsonObject so, Handler<AsyncResult<Void>> next) {

		JsonObject settingInfo = so;
		settingInfo.put(StockReservedConstant.bo_id, "");
		settingInfo.put(StockReservedConstant.account, this.appActivity.getAppInstContext().getAccount());
		settingInfo.put(StockReservedConstant.sku, so.getString("sku"));
		settingInfo.put(StockReservedConstant.warehousecode, so.getString("warehousecode"));
		settingInfo.put(StockReservedConstant.pickoutid, so.getString("pickoutid"));// 拣货出货单ID（状态=拣货）
		settingInfo.put(StockReservedConstant.reserverdnum, so.getString("reservednum"));

		Future<Void> ret = Future.future();
		ret.setHandler(next);

		appActivity.getAppDatasource().getMongoClient().save(appActivity.getDBTableName(appActivity.getName()),
				settingInfo, result -> {
					if (result.succeeded()) {
						ret.complete();
					} else {
						Throwable errThrowable = result.cause();
						String errMsgString = errThrowable.getMessage();
						appActivity.getLogger().error(errMsgString, errThrowable);
						ret.fail(errMsgString);
					}
				});

	}

	private void isCanReserved(Double onhandnum, JsonObject so, Handler<AsyncResult<Void>> next) {
		// TODO 根据条件sum mongodb中数量

		
	}

	private String stockOnHandNullVal(JsonObject so) {
		StringBuffer errors = new StringBuffer();

		String warehouses = so.getString("warehousecode");
		if (null == warehouses || warehouses.equals("")) {
			errors.append("仓库");
		}

		String sku = so.getString("sku");
		if (null == sku || sku.equals("")) {
			errors.append("sku");
		}

		String goods = so.getString("goodaccount");
		if (null == goods || goods.equals("")) {
			errors.append("商品");
		}

		String reservednum = so.getString("reservednum");
		if (null == reservednum || reservednum.equals("")) {
			errors.append("预留量");
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

		ActionURI uri = new ActionURI(ADDRESS, HttpMethod.POST);
		handlerDescriptor.setRestApiURI(uri);

		return actionDescriptor;
	}

}
