package ocr.inventorycenter.safestock;

import java.util.List;

import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.FindOptions;
import ocr.common.handler.SampleSingleDocQueryHandler;
import otocloud.common.ActionURI;
import otocloud.framework.app.function.ActionDescriptor;
import otocloud.framework.app.function.AppActivityImpl;
import otocloud.framework.core.HandlerDescriptor;
import otocloud.framework.core.OtoCloudBusMessage;

/**
 * 库存中心：安全库存-查询
 * 
 * @date 2016年11月20日
 * @author wanghw
 */
// 业务活动功能处理器
public class SafeStockQuery4WarningHandler extends SampleSingleDocQueryHandler {

	public static final String ADDRESS = SafeStockConstant.Query4WarningAddressConstant;

	public SafeStockQuery4WarningHandler(AppActivityImpl appActivity) {
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

		JsonObject query = new JsonObject();

		FindOptions findOptions = new FindOptions();

		appActivity.getAppDatasource().getMongoClient().findWithOptions(
				appActivity.getDBTableName(this.appActivity.getBizObjectType()), query, findOptions, findRet -> {
					if (findRet.succeeded()) {
						List<JsonObject> retObj = findRet.result();
						msg.reply(retObj);
					} else {
						Throwable err = findRet.cause();
						String errMsg = err.getMessage();
						appActivity.getLogger().error(errMsg, err);
						msg.fail(500, errMsg);
					}

				});

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
