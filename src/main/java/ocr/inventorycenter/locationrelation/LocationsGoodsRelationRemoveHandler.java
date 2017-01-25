package ocr.inventorycenter.locationrelation;


import io.vertx.core.json.JsonObject;
import ocr.common.handler.SampleSingleDocBaseHandler;
import otocloud.framework.app.function.AppActivityImpl;
import otocloud.framework.core.OtoCloudBusMessage;
/**
 * 货位商品关系档案
 * 
 * @date 2016年11月20日
 * @author LCL
 */
//业务活动功能处理器
public class LocationsGoodsRelationRemoveHandler  extends SampleSingleDocBaseHandler {
	

	public LocationsGoodsRelationRemoveHandler(AppActivityImpl appActivity) {
		super(appActivity);
		// TODO Auto-generated constructor stub
	}

	// 处理器
	@Override
	public void handle(OtoCloudBusMessage<JsonObject> msg) {
		
		JsonObject query = msg.body();

		appActivity.getAppDatasource().getMongoClient().find(appActivity.getDBTableName(appActivity.getBizObjectType()),
				query,
				result -> {
					if (result.succeeded()) {
						msg.reply(result.result());
					} else {
						Throwable errThrowable = result.cause();
						String errMsgString = errThrowable.getMessage();
						appActivity.getLogger().error(errMsgString, errThrowable);
						msg.fail(100, errMsgString);
					}
				});
	}

	@Override
	public String getEventAddress() {
		// TODO Auto-generated method stub
		return LocationsGoodsRelationConstant.REMOVE_ADDRESS;
	}
	
}
