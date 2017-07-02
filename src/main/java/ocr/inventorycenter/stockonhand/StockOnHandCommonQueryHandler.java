package ocr.inventorycenter.stockonhand;

import io.vertx.core.json.JsonObject;
import ocr.common.handler.SampleDocQueryHandler;
import otocloud.framework.app.function.AppActivityImpl;
import otocloud.framework.core.CommandMessage;

/**
 * 为收银系统提供的查询商品的方法
 * 
 * @author wanghw
 *
 */
public class StockOnHandCommonQueryHandler extends SampleDocQueryHandler {

	public StockOnHandCommonQueryHandler(AppActivityImpl appActivity) {
		super(appActivity);
		// TODO Auto-generated constructor stub
	}

	// 处理器
	@Override
	public void handle(CommandMessage<JsonObject> msg) {
		
		JsonObject query = msg.getContent();

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
		return StockOnHandConstant.CommonQueryAddressConstant;
	}

}