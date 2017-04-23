package ocr.inventorycenter.invfacility;

import java.util.ArrayList;
import java.util.List;

import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import ocr.common.handler.SampleSingleDocQueryHandler;
import otocloud.common.ActionURI;
import otocloud.common.OtoCloudDirectoryHelper;
import otocloud.framework.app.function.ActionDescriptor;
import otocloud.framework.app.function.AppActivityImpl;
import otocloud.framework.core.HandlerDescriptor;
import otocloud.framework.core.CommandMessage;

/**
 * 货架货位
 * 
 * @author LCL
 *
 */
// 业务活动功能处理器
public class InvFacilityTreeQueryHandler extends SampleSingleDocQueryHandler {

	public static final String ADDRESS = "findtree";

	public InvFacilityTreeQueryHandler(AppActivityImpl appActivity) {
		super(appActivity);

	}

	// 此action的入口地址
	@Override
	public String getEventAddress() {
		return ADDRESS;
	}

	// 处理器
	@Override
	public void handle(CommandMessage<JsonObject> msg) {
		
		
//		JsonObject query = msg.body();
		
		JsonObject query = msg.getContent();
		if(query.containsKey("query")){
			query = query.getJsonObject("query");
		}

		appActivity.getAppDatasource().getMongoClient().find(appActivity.getDBTableName(appActivity.getBizObjectType()),
				query, result -> {

					if (result.succeeded()) {
						List<JsonObject> facilitytrees = new ArrayList<JsonObject>();
						List<JsonObject> facilitytree = result.result();

						if (facilitytree != null && facilitytree.size() > 0) {

							for (JsonObject tt : facilitytree) {
								JsonObject parent = new JsonObject();
								String code = (String) tt.getValue("code");
								parent.put("id", code);
								parent.put("text", code);
								JsonObject attributes = new JsonObject();
								attributes.put("code", code);
								attributes.put("name", code);
								attributes.put("inner_code", code);
								parent.put("attributes", attributes);
								parent.put("state", "closed");

								JsonArray details = (JsonArray) tt.getValue("detail");
								JsonObject children = new JsonObject();
								JsonArray treesc = new JsonArray();
								for (int i = 0; i < details.size(); i++) {
									String loccode = (String) details.getJsonObject(i).getValue("loccode");
									children.put("id", loccode);
									children.put("text", loccode);
									treesc.add(children);
								}
								parent.put("children", treesc);

								facilitytrees.add(parent);

							}
							msg.reply(new JsonArray(facilitytrees));
						} else {
							msg.reply(null);
						}
					} else {
						Throwable err = result.cause();
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
