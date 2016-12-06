package ocr.inventorycenter.stockout;

import ocr.common.handler.SampleBillBaseQueryHandler;
import otocloud.framework.app.function.AppActivityImpl;

/**
 * 拣货出库单
 * 
 * @author LCL
 *
 */
public class StockOutQueryHandler extends SampleBillBaseQueryHandler {

	public static final String ADDRESS = StockOutConstant.QueryAddressConstant;

	public StockOutQueryHandler(AppActivityImpl appActivity) {
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
	@Override
	public String getStatus() {
		// TODO Auto-generated method stub
		return StockOutConstant.PickOutedStatus;
	}

}
