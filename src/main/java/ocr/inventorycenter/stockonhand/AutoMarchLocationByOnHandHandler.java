package ocr.inventorycenter.stockonhand;

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
 * 库存中心：现存量查询，货位参照
 * 
 * @date 2016年11月20日
 * @author LCL
 */
// 业务活动功能处理器
public class AutoMarchLocationByOnHandHandler extends ActionHandlerImpl<JsonObject> {

	// 查询方法中间变量
	public static final String ADDRESS = "automatch_location";

	private static final String NYNUM2 = "nynum";
	private static final String ONHANDNUM2 = "onhandnum";
	private static final String LOCATIONCODE = "locationcode";

	public AutoMarchLocationByOnHandHandler(AppActivityImpl appActivity) {
		super(appActivity);

	}

	// 此action的入口地址
	@Override
	public String getEventAddress() {
		return ADDRESS;
	}

	@Override
	public void handle(OtoCloudBusMessage<JsonObject> event) {

		StockOnHandQueryBySkuHandler hander = new StockOnHandQueryBySkuHandler(this.appActivity);
		hander.getLocationsBySku(event, event.body(), next -> {
			if (next.succeeded()) {
				
				JsonArray locations = next.result();
				if (locations == null || locations.size() == 0) {
					event.reply(new JsonArray());
					return;
				}

				JsonArray marchLos = getLocations(event, locations);
				event.reply(marchLos);
			} else {
				Throwable errThrowable = next.cause();
				event.fail(100, errThrowable.getMessage());
			}

		});

	}

	private JsonArray getLocations(OtoCloudBusMessage<JsonObject> event, JsonArray loactions) {
		JsonArray resultlocations = new JsonArray();
		Double nynum = event.body().getDouble(NYNUM2);
		Double sum = 0.0;
		for (Object r : loactions) {
			JsonObject loaction = (JsonObject) r;
			Double onhandnum = loaction.getDouble(ONHANDNUM2);
			if (nynum.compareTo(onhandnum) <= 0) {// 现存量大于拣货数量
				setResultsLocations(resultlocations, loaction);
				break;
			}
			if (nynum.compareTo(sum) <= 0) { // 已经够拣货数量
				break;
			}

			sum = sum + onhandnum;
			setResultsLocations(resultlocations, loaction);
		}
		return resultlocations;
	}

	private void setResultsLocations(JsonArray resultlocations, JsonObject loaction) {
		JsonObject lo = new JsonObject();
		lo.put(LOCATIONCODE, loaction.getString(LOCATIONCODE));
		resultlocations.add(lo);

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
