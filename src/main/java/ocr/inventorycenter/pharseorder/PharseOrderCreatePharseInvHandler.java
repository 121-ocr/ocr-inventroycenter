package ocr.inventorycenter.pharseorder;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import io.vertx.core.Future;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import otocloud.common.ActionURI;
import otocloud.framework.app.function.ActionDescriptor;
import otocloud.framework.app.function.ActionHandlerImpl;
import otocloud.framework.app.function.AppActivityImpl;
import otocloud.framework.app.function.BizRootType;
import otocloud.framework.app.function.BizStateSwitchDesc;
import otocloud.framework.core.HandlerDescriptor;
import otocloud.framework.core.OtoCloudBusMessage;

/**
 * 生成采购入库
 * 
 * @date 2016年11月20日
 * @author LCL
 */
// 业务活动功能处理器
public class PharseOrderCreatePharseInvHandler extends ActionHandlerImpl<JsonArray> {

	private static final String PHARSEINFO = "pharseinfo";
	public static final String ADDRESS = PharseOrderConstant.CreatePharseInvAddressConstant;

	public PharseOrderCreatePharseInvHandler(AppActivityImpl appActivity) {
		super(appActivity);

	}

	// 此action的入口地址
	@Override
	public String getEventAddress() {

		return ADDRESS;
	}

	@Override
	public void handle(OtoCloudBusMessage<JsonArray> msg) {

		JsonArray pharseInvInfo = msg.body();

		JsonObject completionPhaseInfo = new JsonObject();

		String pharseno = getPharseNo(pharseInvInfo);

		Future<Void> nextFuture = Future.future(); // 规定步骤1、2之间的顺序

		// 步骤一、根据采购订单主键查询全集采购订单数据
		getPharseOrderInfo(pharseno, completionPhaseInfo, nextFuture);

		// 步骤二，与步骤一串行的 ，根据传递来的数据进行根据订单号+仓库进行分单退出采购入库单，并记录 来源id 为采购订单。
		nextFuture.setHandler(nextHandler -> {
			createPharseInvs(pharseInvInfo, completionPhaseInfo, msg);
		});

	}

	private String getPharseNo(JsonArray sos) {
		if (null == sos || sos.isEmpty()) {
			return null;
		}

		String pharseno = null;
		for (Object so : sos) {
			JsonObject bo = (JsonObject) so;
			pharseno = bo.getString("invref_boid");
		}
		return pharseno;
	}

	private void createPharseInvs(JsonArray pharseInvInfo, JsonObject completionPhaseInfo,
			OtoCloudBusMessage<JsonArray> msg) {

		Map<String, JsonArray> pharseinvsByWarehouses = new HashMap<String, JsonArray>();

		Map<Integer, JsonObject> completionDetails = getPharseDetailView(completionPhaseInfo);
		Map<String,JsonObject> warehouses = new HashMap<String, JsonObject>();
		
		for (Object so : pharseInvInfo) {
			JsonObject bo = (JsonObject) so;
			String detailcodeString = bo.getString("invref_detailcode");
			Integer detailcode = Integer.getInteger(detailcodeString);
			JsonObject wWarehouse = bo.getJsonObject("invref_warehouse");
			String warehouseCode = wWarehouse.getString("code");
			String key = warehouseCode;
			warehouses.put(key, wWarehouse);
			if (pharseinvsByWarehouses.containsKey(key)) {
				JsonObject jsonObject = completionDetails.get(detailcode);
				jsonObject.put("nynum", bo.getFloat("invref_nynum"));
				jsonObject.put("nsnum", bo.getFloat("invref_nsnum"));
				jsonObject.put("unqualifiednum", bo.getFloat("invref_unqualifiednum"));
				jsonObject.put("locations", bo.getJsonObject("invref_location"));
				jsonObject.put("resid", bo.getString("invref_detailcode"));
				JsonArray stockDetails = pharseinvsByWarehouses.get(key);
				stockDetails.add(jsonObject);
				pharseinvsByWarehouses.put(key, stockDetails);
			} else {
				JsonArray stockDetails = new JsonArray();
				JsonObject jsonObject = completionDetails.get(detailcode);
				jsonObject.put("nynum", bo.getFloat("invref_nynum"));
				jsonObject.put("nsnum", bo.getFloat("invref_nsnum"));
				jsonObject.put("unqualifiednum", bo.getFloat("invref_unqualifiednum"));
				jsonObject.put("locations", bo.getJsonObject("invref_location"));
				jsonObject.put("resid", bo.getString("invref_detailcode"));
				stockDetails.add(jsonObject);

				pharseinvsByWarehouses.put(key, stockDetails);
			}

		}

		JsonArray pharseInvs = new JsonArray();
		JsonObject pharseInvHead = getPharseHeadInfo(completionPhaseInfo);
		
		for (Entry<String, JsonArray> pharseinvsByWarehouse : pharseinvsByWarehouses.entrySet()) {
			String key = pharseinvsByWarehouse.getKey();
			pharseInvHead.put("warehouse", warehouses.get(key));
					
			JsonArray details = pharseinvsByWarehouse.getValue();	
			pharseInvHead.put("detail", details);
			
			pharseInvs.add(pharseInvHead);
		}
		
		DeliveryOptions options = new DeliveryOptions();
		options.setHeaders(msg.headers());
		this.appActivity.getEventBus().send(getPharseOrderAddress(), pharseInvs, facilityRes -> {
			if (facilityRes.succeeded()) {
				if (facilityRes.succeeded()) {
					msg.reply(facilityRes.result().body());
				} else {
					Throwable errThrowable = facilityRes.cause();
					String errMsgString = errThrowable.getMessage();
					appActivity.getLogger().error(errMsgString, errThrowable);
					msg.fail(100, errMsgString);
				}

			}

		});
	}

	private JsonObject getPharseHeadInfo(JsonObject completionPhaseInfo) {
		JsonObject completionHeadInfo = completionPhaseInfo.copy();
		JsonObject phraseOjb = completionHeadInfo.getJsonObject("pharseinfo").getJsonObject("bo");
		phraseOjb.remove("detail");
		return phraseOjb;
	}

	private Map<Integer, JsonObject> getPharseDetailView(JsonObject completionPhaseInfo) {
		// completionPhaseInfoView K= 表体主键 V=表体明细数据
		Map<Integer, JsonObject> completionPhaseInfoView = new HashMap<Integer, JsonObject>();
		JsonObject jsonObject = completionPhaseInfo.getJsonObject(PHARSEINFO);

		JsonArray details = jsonObject.getJsonObject("bo").getJsonArray("detail");
		for (Object item : details) {
			JsonObject detail = (JsonObject) item;
			String detailcodeStr = detail.getString("detail_code");
			Integer detailcode = Integer.getInteger(detailcodeStr);
			completionPhaseInfoView.put(detailcode, detail);
		}
		return completionPhaseInfoView;
	}

	private String getPharseOrderAddress() {
		
		String server = this.appActivity.getService().getRealServiceName();
		String address = this.appActivity.getAppInstContext().getAccount() + "." + server + "." + "pharseinv-mgr" + "."
				+ "batch_create";
		return address;
	}

	private String getKey(String no, String warehousecode) {
		String key = no + "-" + warehousecode;
		return key;
	}

	private void getPharseOrderInfo(String pharseno, JsonObject resultObjects, Future<Void> nextFuture) {

		if (null == pharseno || pharseno.equals("")) {
			nextFuture.failed();
			return;
		}

		JsonObject query = new JsonObject();
		query.put("bo_id", pharseno);

		PharseOrderQueryByConditonHandler handler = new PharseOrderQueryByConditonHandler(this.appActivity);

		handler.queryByConditions(query, result -> {
			if (result.succeeded()) {
				JsonObject onhands = result.result();
				resultObjects.put(PHARSEINFO, onhands);
				nextFuture.complete();

			} else {
				Throwable err = result.cause();
				String errMsg = err.getMessage();
				appActivity.getLogger().error(errMsg, err);
				nextFuture.failed();
			}
		});
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public ActionDescriptor getActionDesc() {

		ActionDescriptor actionDescriptor = super.getActionDesc();
		HandlerDescriptor handlerDescriptor = actionDescriptor.getHandlerDescriptor();

		// 外部访问url定义
		ActionURI uri = new ActionURI(ADDRESS, HttpMethod.POST);
		handlerDescriptor.setRestApiURI(uri);

		// 状态变化定义
		BizStateSwitchDesc bizStateSwitchDesc = new BizStateSwitchDesc(BizRootType.BIZ_OBJECT,
				PharseOrderConstant.CreatedStatus, PharseOrderConstant.InvStatus);
		bizStateSwitchDesc.setWebExpose(true); // 是否向web端发布事件
		actionDescriptor.setBizStateSwitch(bizStateSwitchDesc);

		return actionDescriptor;
	}

}
