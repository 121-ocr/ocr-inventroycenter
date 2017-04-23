package ocr.inventorycenter.stockonhand;

import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import otocloud.common.ActionURI;
import otocloud.framework.app.function.ActionDescriptor;
import otocloud.framework.app.function.ActionHandlerImpl;
import otocloud.framework.app.function.AppActivityImpl;
import otocloud.framework.core.HandlerDescriptor;
import otocloud.framework.core.CommandMessage;

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

	private static final String NSNUM2 = "nsnum";
	private static final String PLUSUM2 = "plusnum";
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
	public void handle(CommandMessage<JsonObject> event) {

		JsonObject params = event.getContent();
		
		StockOnHandQueryBySkuHandler hander = new StockOnHandQueryBySkuHandler(this.appActivity);
		hander.getLocationsBySku(params, next -> {
			
			if (next.succeeded()) {			
				JsonArray locations = next.result();
				if (locations == null || locations.size() == 0) {
					event.reply(new JsonArray());
					return;
				}
				JsonArray marchLos = getLocations(params, locations);
				event.reply(marchLos);
			} else {
				Throwable errThrowable = next.cause();
				event.fail(100, errThrowable.getMessage());
			}

		});

	}

	private JsonArray getLocations(JsonObject params, JsonArray loactions) {
		JsonArray resultlocations = new JsonArray();

		Double nsnum = getnsnum(params);
		
		//拣货剩余量
		Double plusnum = nsnum;
		for (Object r : loactions) {
			JsonObject loaction = (JsonObject) r;
			Double locationPlusnum = loaction.getDouble(PLUSUM2); //货位剩余量
			if (plusnum.compareTo(locationPlusnum) <= 0) {// 货位剩余量大于拣货剩余量
				loaction.put("nsnum", plusnum);
				setResultsLocations(resultlocations, loaction);
				break;
			}
			
			loaction.put("nsnum", locationPlusnum);
			setResultsLocations(resultlocations, loaction);
			
			plusnum = plusnum - locationPlusnum;
			if(plusnum <= 0.0){
				break;
			}
		}
		return resultlocations;
	}

	private Double getnsnum(JsonObject params) {
		Double nsnum =  params.getJsonObject("query").getDouble(NSNUM2);
		//Double nynum =Double.parseDouble(nynumsrt);
		return nsnum;
	}

	private void setResultsLocations(JsonArray resultlocations, JsonObject loaction) {
		//JsonObject lo = new JsonObject();
		//lo.put(LOCATIONCODE, loaction.getString(LOCATIONCODE));
		resultlocations.add(loaction);

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
