package ocr.inventorycenter.pharseinv;


import ocr.common.handler.SampleBillBaseQueryHandler;
import otocloud.framework.app.function.AppActivityImpl;
/**
 * 库存中心：采购入库-查询
 * 
 * @date 2016年11月20日
 * @author LCL
 */
//业务活动功能处理器
public class PharseInvQueryHandler  extends SampleBillBaseQueryHandler {

	public static final String ADDRESS = PharseInvConstant.QueryAddressConstant;

	public PharseInvQueryHandler(AppActivityImpl appActivity) {
		super(appActivity);
		// TODO Auto-generated constructor stub
	}

	// 此action的入口地址
	@Override
	public String getEventAddress() {
		// TODO Auto-generated method stub
		return ADDRESS;
	}

	/**
	 * 要查询的单据状态
	 * 
	 * @return
	 */
	public String getStatus() {
		// TODO Auto-generated method stub
 	return PharseInvConstant.CreatedStatus;
		//return msgBody.getString("status");
	}

}
