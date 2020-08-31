package com.inspur.plugins.timedtask.service.impl;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jws.WebService;
import javax.ws.rs.ext.MessageBodyWriter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.util.DigestUtils;

import com.cmcc.esb.vispractice.ws.MsgHeader.MsgHeader;
import com.cmcc.soa.MsgHeader.MSGHEADER;
import com.inspur.cost.QueryInputParamCostant;
import com.inspur.cost.WSCostUtil;
import com.inspur.cost.WSCostUtilToll;
import com.inspur.plugins.bnsWorkFlow.model.TWfBnsWoLine;
import com.inspur.plugins.bnsWorkFlow.model.TWfBnsWoMain;
import com.inspur.plugins.iGrpAtBnsType.dao.IGrpAtBnsTypeDao;
import com.inspur.plugins.iGrpAtBnsType.model.IGrpAtBnsType;
import com.inspur.plugins.iGrpBgtBase.dao.IGrpBgtBaseDao;
import com.inspur.plugins.iGrpBgtBase.model.IGrpBgtBase;
import com.inspur.plugins.iGrpBgtMultiRela.dao.IGrpBgtMultiRelaDao;
import com.inspur.plugins.iGrpBgtMultiRela.model.IGrpBgtMultiRela;
import com.inspur.plugins.iGrpBgtProject.dao.IGrpBgtProjectDao;
import com.inspur.plugins.iGrpBgtProject.model.IGrpBgtProject;
import com.inspur.plugins.iGrpBgtProjectOrgRela.dao.IGrpBgtProjectOrgRelaDao;
import com.inspur.plugins.iGrpBgtProjectOrgRela.model.IGrpBgtProjectOrgRela;
import com.inspur.plugins.iGrpCtrAttr.dao.IGrpCtrAttrDao;
import com.inspur.plugins.iGrpCtrAttr.model.IGrpCtrAttr;
import com.inspur.plugins.iGrpCtrBase.dao.IGrpCtrBaseDao;
import com.inspur.plugins.iGrpCtrBase.model.IGrpCtrBase;
import com.inspur.plugins.iGrpCtrBudget.dao.IGrpCtrBudgetDao;
import com.inspur.plugins.iGrpCtrBudget.model.IGrpCtrBudget;
import com.inspur.plugins.iGrpErpZc.dao.IGrpErpZcDao;
import com.inspur.plugins.iGrpErpZc.model.IGrpErpZc;
import com.inspur.plugins.iGrpErpZcBook.dao.IGrpErpZcBookDao;
import com.inspur.plugins.iGrpErpZcBook.model.IGrpErpZcBook;
import com.inspur.plugins.iGrpErpZcBook.service.IIGrpErpZcBookService;
import com.inspur.plugins.igrperpsb.dao.IGrpErpOuDao;
import com.inspur.plugins.igrperpsb.dao.IGrpErpSbjectBalDao;
import com.inspur.plugins.igrperpsb.dao.IGrpErpSbjectDao;
import com.inspur.plugins.igrperpsb.model.IGrpErpOu;
import com.inspur.plugins.igrperpsb.model.IGrpErpSbject;
import com.inspur.plugins.igrperpsb.model.IGrpErpSbjectBal;
import com.inspur.plugins.tTokenInfo.service.ITTokenInfoService;
import com.inspur.plugins.tgrppurchase.dao.TGrpPurchasePoDao;
import com.inspur.plugins.tgrppurchase.dao.TGrpPurchasePolineDao;
import com.inspur.plugins.tgrppurchase.model.TGrpPurchasePo;
import com.inspur.plugins.tgrppurchase.model.TGrpPurchasePoline;
import com.inspur.plugins.timedtask.service.taskJobService;

import net.sf.jsqlparser.statement.select.Select;

/**
 * @author hanlq
 *
 */
@WebService
@Service("com.inspur.plugins.timedtask.service.impl.TaskJobServiceImpl")
@Transactional(readOnly = true)
public class TaskJobServiceImpl implements taskJobService {

	@Autowired
	private JdbcTemplate jdbcTemplate;
	
	@Autowired
    private DataSourceTransactionManager txManager;
	
	@Autowired
	private TGrpPurchasePoDao tgrppurchasepoDao;
	
	@Autowired
	private TGrpPurchasePolineDao tgrppurchasepolineDao;
	
	@Autowired
	private IGrpAtBnsTypeDao igrpatbnstypeDao;
	
	@Autowired
	private IGrpErpSbjectBalDao igrperpsbjectbalDao;
	
	@Autowired
	private IGrpErpSbjectDao igrperpsbjectDao;
	
	@Autowired
	private IGrpErpOuDao igrperpouDao;
	
	@Autowired
	private IGrpErpZcBookDao igrperpzcbookDao;

	@Autowired
	private IGrpErpZcDao igrperpzcDao;
	
	@Autowired
	private IGrpBgtProjectDao igrpbgtprojectDao;
	
	@Autowired
	private IGrpBgtProjectOrgRelaDao igrpbgtprojectorgrelaDao;
	
	@Autowired
	private IGrpBgtMultiRelaDao igrpbgtmultirelaDao;
	
	@Autowired
	private IGrpCtrAttrDao igrpctrattrDao;
	
	@Autowired
	private IGrpCtrBudgetDao igrpctrbudgetDao;
	
	@Autowired
	private IGrpCtrBaseDao igrpctrbaseDao;
	
	@Autowired
	private IGrpBgtBaseDao igrpbgtbaseDao;
	
	@Autowired
	private ITTokenInfoService ittokeninfoService;
	
	@Autowired
	private IIGrpErpZcBookService igrperpzcbookService;
    /**
     * @author hanlq
     * 
     * 定时推送数据库中所有未推送报账的冲销报账单
     */
	@Override
    public void writeOffTaskJob() {
    	StringBuffer mainNumSql=new StringBuffer();
    	mainNumSql.append("select Count(1) from T_WF_BNS_WO_MAIN  where PUSH_STATE is null or PUSH_STATE='失败'");
    	int mainNum=this.jdbcTemplate.queryForInt(mainNumSql.toString());
    	if (mainNum>0) {
    		 StringBuffer mainListSql=new StringBuffer();
        	 mainListSql.append("select ID,WF_NO,WD_NO,COST_TYPE,COST_TYPE_NAME,CITY,COUNTY,DEPARTMENT,COST_CENTER,WO_STATE,WO_MONTH,FINANCE_NO,WD_YD,WD_JK,WD_ZQ,WD_XYW,WD_SPLIT_BY,PUSH_STATE,PRI_KEY,BATCH_NUMBER,OPR_EMPLOYEE_CODE,PHONE_NUMBER,OPR_COMPANY_CODE,OPR_UNIT_CODE,SOURCE_SYSTEM_CODE,SOURCE_DOCUMENT_NUM,BUSINESS_MAJOR_CLASS_CODE,SUMMARY,VENDOR_NUM,TOTAL_AMOUNT,CURRENCY_CODE,CLAIM_TYPE_CODE,DESCRIPTION,CONTAIN_CONTRACT_FLAG,CONTRACT_NUMBER,CONTRACT_CURRENCY,PAYABLE_ACCOUNT_TYPE,ATTACHMENT_NUMBER,PAYABLE_FLAG,INVOICE_TYPE_CODE,ATTRIBUTE1,ATTRIBUTE2,ATTRIBUTE3,ATTRIBUTE4,ATTRIBUTE5,ATTRIBUTE6,ATTRIBUTE7,ATTRIBUTE8,ATTRIBUTE9,ATTRIBUTE10,ATTRIBUTE11,ATTRIBUTE12,ATTRIBUTE13,ATTRIBUTE14,ATTRIBUTE15,INPUT_EXT,ORDER_GEN_STATUS,TIME_STAMP from T_WF_BNS_WO_MAIN  where PUSH_STATE is null or PUSH_STATE='失败'");
        	List<TWfBnsWoMain> tWfBnsWoMains= new ArrayList<TWfBnsWoMain>();
    		tWfBnsWoMains =this.jdbcTemplate.query(mainListSql.toString(), new BeanPropertyRowMapper<TWfBnsWoMain>(TWfBnsWoMain.class));
        	for (TWfBnsWoMain tWfBnsWoMain : tWfBnsWoMains) {
        		StringBuffer lineListSql=new StringBuffer();
        		lineListSql.append("select id,wf_no,pri_key,source_doc_line_num,business_major_class_code,payable_account_type,vendor_num,contract_number,cost_center_code,business_minor_class_code,business_activity_code,amount,summary,currency_code,currency_rate,currency_amount,market_code,product_code,purchase_object_code,mcbackup1,mcbackup2,mcbackup3,budget_key,budget_company_code,budget_contract_header,budget_contract_line,line_amount,line_tax_amount,amortize_start_period,amortize_end_period,amortize_month,budget_date,parent_expense_line_id,attribute1,attribute2,attribute3,attribute4,attribute5,attribute6,attribute7,attribute8,attribute9,attribute10,attribute11,attribute12,attribute13,attribute14,attribute15,input_ext from T_WF_BNS_WO_LINE where WF_NO='"+tWfBnsWoMain.getWfNo()+"'");
        		List<TWfBnsWoLine> tWfBnsWoLines=this.jdbcTemplate.query(lineListSql.toString(), new BeanPropertyRowMapper<TWfBnsWoLine>(TWfBnsWoLine.class));
        		try {
        			Map<String, Object> obj =ittokeninfoService.writeOff(tWfBnsWoMain, tWfBnsWoLines);
        			if((boolean) obj.get("success")) {
        				this.jdbcTemplate.update("update T_WF_BNS_WO_MAIN set PUSH_STATE='成功' where WF_NO='"+tWfBnsWoMain.getWfNo()+"'");
        				//待补充
        				//Map<String, Object> map = ittokeninfoService.queryBillStatus(tWfBnsWoMain.getWfNo());
        			}else {
        				this.jdbcTemplate.update("update T_WF_BNS_WO_MAIN set PUSH_STATE='失败' where WF_NO='"+tWfBnsWoMain.getWfNo()+"'");
        				System.out.println("冲销接口推送失败，失败原因为："+obj.get("message"));
        			}
    			} catch (Exception e) {
    				StringBuffer errorSql=new StringBuffer();
    				errorSql.append("update T_WF_BNS_WO_MAIN set PUSH_STATE='失败' where WF_NO='"+tWfBnsWoMain.getWfNo()+"'");
    				this.jdbcTemplate.execute(errorSql.toString());
    				System.out.println("冲销推送出错,错误日志为：");
    				e.printStackTrace();
    			}
    		}
		} else {
            System.out.println("不存在未冲销数据！");
		}
    }
	
	/**
	 * 查询供应链采购订单信息接口--分页
	 */
	@Override
	public void queryPagePurchasePoInfoSrvTaskJob() {
		try {
			com.cmcc.esb.vispractice.ws.SB_SC_SCM_PageInquiryPurchasePoInfoSrv.SB_SC_SCM_PageInquiryPurchasePoInfoSrvRequest input = new com.cmcc.esb.vispractice.ws.SB_SC_SCM_PageInquiryPurchasePoInfoSrv.SB_SC_SCM_PageInquiryPurchasePoInfoSrvRequest();
			com.cmcc.esb.vispractice.ws.MsgHeader.MsgHeader msgHeader = new MsgHeader();
			//增量查询
			input.setLAST_UPDATE_START(WSCostUtilToll.getStartTimeCal());
			input.setLAST_UPDATE_END(WSCostUtilToll.getEndTimeCal());
			msgHeader.setSOURCE_SYSTEM_ID(QueryInputParamCostant.SOURCESYSTEMID);
			msgHeader.setSOURCE_SYSTEM_NAME(QueryInputParamCostant.SOURCESYSTEMNAME);
			msgHeader.setTARGET_SYSTEM_ID("SD-LS");
			msgHeader.setTARGET_SYSTEM_NAME("山东业务台账系统");
			// 提交时间（服务触发时间）
			SimpleDateFormat dFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			System.out.println("查询采购订单信息接口服务触发时间" + dFormat.format(new Date()));
			msgHeader.setSUBMIT_DATE(dFormat.format(new Date()));
			BigDecimal pageSize = BigDecimal.valueOf(20);
			msgHeader.setPAGE_SIZE(pageSize);
			// 当前页
			int currentPage = 1;
			// 总记录数
			BigDecimal totalRecord = BigDecimal.valueOf(-1);
			// 计算总页数
			BigDecimal totalPage = new BigDecimal(0);
			com.cmcc.esb.vispractice.ws.SB_SC_SCM_PageInquiryPurchasePoInfoSrv.SB_SC_SCM_PageInquiryPurchasePoInfoSrvResponse output = null;
			do {
				msgHeader.setCURRENT_PAGE(BigDecimal.valueOf(currentPage));
				msgHeader.setTOTAL_RECORD(totalRecord);
				input.setMsgHeader(msgHeader);
				output = WSCostUtil.processPageInquiryPurchasePoInfoSrv(input);
				// 获取总记录数
				totalRecord = output.getTOTAL_RECORD();
				System.out.println("总记录数：" + totalRecord);
				System.out.println("每页大小：" + output.getPAGE_SIZE());
				System.out.println("返回结果集长度：" + output.getPoHeader().length);
				System.out.println("返回信息状态标识:" + output.getRETURN_FLAG());
				System.out.println("返回消息:" + output.getRETURN_MESSAGE());
				if ("Y".equalsIgnoreCase(output.getRETURN_FLAG())) {
					totalPage = totalRecord.divide(pageSize, 0, BigDecimal.ROUND_CEILING);
					com.cmcc.esb.vispractice.ws.SB_SC_SCM_PageInquiryPurchasePoInfoSrv.PoHeaderItem[] poHeaderItems = output
							.getPoHeader();
					System.out.println("采购订单数据实体长度：" + poHeaderItems.length);
					for (com.cmcc.esb.vispractice.ws.SB_SC_SCM_PageInquiryPurchasePoInfoSrv.PoHeaderItem poHeaderItem : poHeaderItems) {
						com.cmcc.esb.vispractice.ws.SB_SC_SCM_PageInquiryPurchasePoInfoSrv.PoLineItem[] poLineItems = poHeaderItem
								.getPoLine();
						TGrpPurchasePo tgrppurchasepo = new TGrpPurchasePo();
						tgrppurchasepo = WSCostUtilToll.switchBusinessEntity2WSEntity(poHeaderItem,
								tgrppurchasepo.getClass());
						tgrppurchasepo.setPKey(poHeaderItem.getP_KEY());
						StringBuffer sqlByPKey = new StringBuffer();
						sqlByPKey.append("SELECT COUNT(1)  FROM  T_GRP_PURCHASE_PO WHERE P_KEY='"
								+ poHeaderItem.getP_KEY() + "'");
						int pKeyIsExist = this.jdbcTemplate.queryForInt(sqlByPKey.toString());
						if (pKeyIsExist > 0) {
							System.out.println("进入采购订单头信息更新方法！");
							tgrppurchasepoDao.update(tgrppurchasepo);
							tgrppurchasepolineDao.deletePoLineByPkey(poHeaderItem.getP_KEY());
							System.out.println("采购订单行信息数据实体长度：" + poLineItems.length);
							for (com.cmcc.esb.vispractice.ws.SB_SC_SCM_PageInquiryPurchasePoInfoSrv.PoLineItem poLineItem : poLineItems) {
								TGrpPurchasePoline tgrppurchasepoline = new TGrpPurchasePoline();
								tgrppurchasepoline = WSCostUtilToll.switchBusinessEntity2WSEntity(poLineItem,
										tgrppurchasepoline.getClass());
								tgrppurchasepoline.setPLineKey(poLineItem.getP_LINE_KEY());
								tgrppurchasepoline.setPKey(poLineItem.getP_KEY());
								tgrppurchasepoline.setNeedByDate(0L);
								tgrppurchasepoline.setConfirmDate(0L);
								StringBuffer sqlByPLineKey = new StringBuffer();
								sqlByPLineKey.append("SELECT COUNT(1)  FROM  T_GRP_PURCHASE_POLINE WHERE P_LINE_KEY='"
										+ tgrppurchasepoline.getPLineKey() + "'");
								int pLineKeyIsExist = this.jdbcTemplate.queryForInt(sqlByPLineKey.toString());
								if (pLineKeyIsExist > 0) {
									System.out.println("进入采购订单行信息更新方法！");
									tgrppurchasepolineDao.update(tgrppurchasepoline);
								} else {
									System.out.println("进入采购订单行信息新增方法！");
									tgrppurchasepolineDao.create(tgrppurchasepoline);
								}
							}
						} else {
							System.out.println("进入采购订单头信息新增方法！");
							tgrppurchasepoDao.create(tgrppurchasepo);
							System.out.println("采购订单行信息数据实体长度：" + poLineItems.length);
							for (com.cmcc.esb.vispractice.ws.SB_SC_SCM_PageInquiryPurchasePoInfoSrv.PoLineItem poLineItem : poLineItems) {
								TGrpPurchasePoline tgrppurchasepoline = new TGrpPurchasePoline();
								tgrppurchasepoline = WSCostUtilToll.switchBusinessEntity2WSEntity(poLineItem,
										tgrppurchasepoline.getClass());
								tgrppurchasepoline.setPKey(poLineItem.getP_KEY());
								tgrppurchasepoline.setPLineKey(poLineItem.getP_LINE_KEY());
								StringBuffer sqlByPLineKey = new StringBuffer();
								sqlByPLineKey.append("SELECT COUNT(1)  FROM  T_GRP_PURCHASE_POLINE WHERE P_LINE_KEY='"
										+ tgrppurchasepoline.getPLineKey() + "'");
								int pLineKeyIsExist = this.jdbcTemplate.queryForInt(sqlByPLineKey.toString());
								if (pLineKeyIsExist > 0) {
									System.out.println("进入采购订单行信息更新方法！");
									tgrppurchasepolineDao.update(tgrppurchasepoline);
								} else {
									System.out.println("进入采购订单行信息新增方法！");
									tgrppurchasepolineDao.create(tgrppurchasepoline);
								}
							}
						}
					}
				} else {
					System.out.println("返回信息状态标识:" + output.getRETURN_FLAG());
					System.out.println("返回消息:" + output.getRETURN_MESSAGE());
					break;
				}
				System.out.println("查询第" + currentPage + "页执行完毕！总共" + totalPage.intValue() + "页");
				currentPage++;
			} while (currentPage <= totalPage.intValue());
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("查询采购订单接口执行出错！");
		}
	}

    /**
	 * OSB_RBS_CMF_HQ_00059_查询报账单状态信息服务_V0.1（分页）
	 * 因为报账侧作废后二次推送报账系统会生成不同的报账单编号，查询存在问题，所以此接口弃用！
	 * @param input
	 * @return
	 * @throws Exception
	 */
	@Override
	public void queryBillStatusTimeJob(String billType){
		System.out.println("***********查询报账单状态信息服务_V0.1（分页）**********");
		try {
			//查询所有已经推送报账并且未结束的报账单
			StringBuffer selectSql=new StringBuffer();
			
			switch (billType) {
			case "AT":
				selectSql.append("select source_document_num  from  t_wf_bns_at_main where push_state='成功' and at_state <> 'COMPLETED' and at_state <> 'ABOLISHED'");
				break;
			case "WD":
				selectSql.append("select source_document_num  from  t_wf_bns_wd_main where push_state='成功' and wd_state <> 'COMPLETED' and wd_state <> 'ABOLISHED'");
				break;
			case "WO":
				selectSql.append("select source_document_num  from  t_wf_bns_wo_main where push_state='成功' and wo_state <> 'COMPLETED' and wo_state <> 'ABOLISHED'");
				break;
			default:
				break;
			}
			List<String> atList=this.jdbcTemplate.queryForList(selectSql.toString(),String.class);
			/*条件示例*/
			//List<String> atList = new ArrayList<>();
			//atList.add("来源系统单据号1");
			//atList.add("来源系统单据号2");
			//填充头信息和查询条件
			com.cmcc.soa.OSB_RBS_CMF_HQ_PageInquiryClaimDocStatusSrv.InputParameters input=
					new com.cmcc.soa.OSB_RBS_CMF_HQ_PageInquiryClaimDocStatusSrv.InputParameters();
			com.cmcc.soa.MsgHeader.MSGHEADER msgHeader = ittokeninfoService.getSOAHeader();
			msgHeader.setPROVINCE_CODE(QueryInputParamCostant.PROVINCE_CODE);
			input.setMSGHEADER(msgHeader);
			input.setQRY_TYPE_CODE(QueryInputParamCostant.QRY_TYPE_CODE);  //Y必填 查询类型代码     CLAIM_DOC-报账单导入
			input.setSOURCE_SYSTEM_CODE(QueryInputParamCostant.SOURCE_SYSTEM_CODE);//Y必填	来源系统代码
			input.setBATCH_COLUMN_NAME(QueryInputParamCostant.BATCH_COLUMN_NAME);//CLAIM_NUM-按多个报账单编号, SOURCE_DOCUMENT_NUM-按多个原单号
			//构建BATCH_CONDITION 批量字段值集数据实体--注意分页
			int listSize=atList.size();
			System.out.println("所有已经推送报账并且未结束的报账单数量为："+listSize);
			BigDecimal listSizeBigDecimal=BigDecimal.valueOf(listSize);
			BigDecimal limitNumBigDecimal=BigDecimal.valueOf(50);
			BigDecimal totalBigDecimal=listSizeBigDecimal.divide(limitNumBigDecimal,0,BigDecimal.ROUND_CEILING);
			int totalPage=totalBigDecimal.intValue();
			int i=0;
			int num=0;
			for (int k = 0; k < totalPage; k++) {
				int BATCH_CONDITION_ITEM_LENGTH=0;
				if(totalPage==k+1) {
					BATCH_CONDITION_ITEM_LENGTH=listSize-(50*k);
				}else {
					BATCH_CONDITION_ITEM_LENGTH=50;
				}
				System.out.println("本次查询的第"+(k+1)+"次循环调用！");
				com.cmcc.soa.OSB_RBS_CMF_HQ_PageInquiryClaimDocStatusSrv.BATCH_CONDITION_ITEM[] BATCH_CONDITION_ITEMs= new com.cmcc.soa.OSB_RBS_CMF_HQ_PageInquiryClaimDocStatusSrv.BATCH_CONDITION_ITEM[BATCH_CONDITION_ITEM_LENGTH];
				for (int j=0 ;i < BATCH_CONDITION_ITEM_LENGTH+k*50; j++) {
					BATCH_CONDITION_ITEMs[j]=new com.cmcc.soa.OSB_RBS_CMF_HQ_PageInquiryClaimDocStatusSrv.BATCH_CONDITION_ITEM(atList.get(i));
					i++;
				}
				input.setBATCH_CONDITION(BATCH_CONDITION_ITEMs);
				com.cmcc.soa.OSB_RBS_CMF_HQ_PageInquiryClaimDocStatusSrv.OutputParameters output = 
						WSCostUtil.processPageInquiryClaimDocStatusSrv(input);
				if ("TRUE".equals(output.getBIZ_SERVICE_FLAG())&&"TRUE".equals(output.getESB_FLAG())) {
					com.cmcc.soa.OSB_RBS_CMF_HQ_PageInquiryClaimDocStatusSrv.OUTPUTCOLLECTION_ITEM [] collection = output.getOUTPUTCOLLECTION();
					System.out.println("分页查询报账单状态接口数组长度"+collection.length);
					num=num+collection.length;
					System.out.println("********************开始循环打印报账单状态数据实体*******************");
					int m = 0;
					for(com.cmcc.soa.OSB_RBS_CMF_HQ_PageInquiryClaimDocStatusSrv.OUTPUTCOLLECTION_ITEM col : collection){
						this.jdbcTemplate.execute("call P_AT_STATE_DEAL('"+billType+"','"+col.getSOURCE_DOCUMENT_NUM()+"','"+col.getSTATUS_CODE()+"','"+col.getFLOW_STATUS_CODE()+"','"+col.getERP_STATUS_CODE()+"','"+col.getAMOUNT()+"','"+col.getREVERSE_FLAG()+"')");
						m++;
						System.out.println("*********************第"+m+"条报账单状态数据******************");
						Class cls = col.getClass();
						Field[] fields = cls.getDeclaredFields();  
						for(int n=0; n<fields.length; n++){  
							Field f = fields[n];  
							f.setAccessible(true);  
							System.out.println("属性名:" + f.getName() + " 属性值:" + f.get(col));  
						}
					}
					System.out.println("********************循环打印报账单状态数据实体结束*******************");
				}else {
					System.out.println("打印ESB 侧执行结果:"+output.getESB_FLAG());
					System.out.println("打印ESB 侧错误代码:"+output.getESB_RETURN_CODE());
					System.out.println("打印系统返回消:息"+output.getESB_RETURN_MESSAGE());
					System.out.println("打印业务服务侧执行结果:"+output.getBIZ_SERVICE_FLAG());
					System.out.println("打印业务服务侧错误代码:"+output.getBIZ_RETURN_CODE());
					System.out.println("打印业务服务返回消息:"+output.getBIZ_RETURN_MESSAGE());
				}
			}
			System.out.println("本次查询共查询到"+num+"条报账单状态信息！");
		} catch (Exception e) {
			System.out.println("*****************************接口执行过程出错****************************");
			e.printStackTrace();
		}
     }
	
	  /**
		 * 查询报账单状态信息服务（不分页）
		 * 因为报账侧作废后二次推送报账系统会生成不同的报账单编号，分页查询存在问题，所以启用此接口单条查询！
		 * @param input
		 * @return
		 * @throws Exception
		 */
		@Override
		public void queryBillStatusTimeSrc(String billType){
			System.out.println("***********查询报账单状态信息服务_V0.1（分页）**********");
			try {
				StringBuffer selectSql=new StringBuffer();
				StringBuffer selectNullSql=new StringBuffer();
				StringBuffer updateSql=new StringBuffer();
				switch (billType) {
				case "AT":
					selectSql.append("select finance_no  from  t_wf_bns_at_main where push_state='成功' and at_state <> 'COMPLETED' and at_state <> 'ABOLISHED' and finance_no is not null");
					selectNullSql.append("select source_document_num from t_wf_bns_at_main where push_state='成功' and at_state <> 'COMPLETED' and at_state <> 'ABOLISHED' and finance_no is null");
					updateSql.append("update t_wf_bns_at_main set finance_no=?, at_state=? where source_document_num=?");
					break;
				case "WD":
					selectSql.append("select finance_no  from  t_wf_bns_wd_main where push_state='成功' and wd_state <> 'COMPLETED' and wd_state <> 'ABOLISHED' and finance_no is not null");
					selectNullSql.append("select source_document_num  from  t_wf_bns_wd_main where push_state='成功' and wd_state <> 'COMPLETED' and wd_state <> 'ABOLISHED' and finance_no is null");
					updateSql.append("update t_wf_bns_wd_main set finance_no=?, wd_state=? where source_document_num=?");
					break;
				case "WO":
					selectSql.append("select finance_no  from  t_wf_bns_wo_main where push_state='成功' and wo_state <> 'COMPLETED' and wo_state <> 'ABOLISHED' and finance_no is not null");
					selectNullSql.append("select source_document_num  from  t_wf_bns_wo_main where push_state='成功' and wo_state <> 'COMPLETED' and wo_state <> 'ABOLISHED' and finance_no is null");
					updateSql.append("update t_wf_bns_wo_main set finance_no=?, wo_state=? where source_document_num=?");
					break;
				default:
					break;
				}
				List<String> financeNoList=this.jdbcTemplate.queryForList(selectSql.toString(),String.class);
				List<String> sysDocNumList=this.jdbcTemplate.queryForList(selectNullSql.toString(),String.class);
				System.out.println("所有已推送但未结束且未作废的报账单数量为："+financeNoList.size());
				System.out.println("所有已推送但未获取报账系统报账单号的报账单数量为："+sysDocNumList.size());
				for (String sysDocNum : sysDocNumList) {
					Map<String, Object> map=ittokeninfoService.queryBillStatus(sysDocNum);
					int countNum=this.jdbcTemplate.update(updateSql.toString(),map.get("claimNum"),map.get("billStatus"),map.get("wfNo"));
					System.out.println("成功更新"+countNum+"条报账单数据！");
				}
			for (String financeNo : financeNoList) {
				com.cmcc.soa.OSB_RBS_CMF_HQ_InquiryClaimDocStatusSrv.InputParameters input=
						new com.cmcc.soa.OSB_RBS_CMF_HQ_InquiryClaimDocStatusSrv.InputParameters();
				//填充头消息
				com.cmcc.soa.MsgHeader.MSGHEADER msgHeader = ittokeninfoService.getSOAHeader();
				msgHeader.setPROVINCE_CODE(QueryInputParamCostant.PROVINCE_CODE);
				input.setMSGHEADER(msgHeader);
				//填充条件
				input.setOPR_COMPANY_CODE(QueryInputParamCostant.OPR_COMPANY_CODE);//报账公司代码    N非必填 
				input.setSOURCE_SYSTEM_CODE(QueryInputParamCostant.SOURCE_SYSTEM_CODE);//来源系统代码   Y必填
				input.setCLAIM_NUM(financeNo);
				com.cmcc.soa.OSB_RBS_CMF_HQ_InquiryClaimDocStatusSrv.OutputParameters output = 
						WSCostUtil.processInquiryClaimDocStatusSrv(input);
				com.cmcc.soa.OSB_RBS_CMF_HQ_InquiryClaimDocStatusSrv.OUTPUTCOLLECTION_ITEM [] collection = output.getOUTPUTCOLLECTION();
				Map<String, Object> map=new HashMap<>();
				if ("TRUE".equals(output.getBIZ_SERVICE_FLAG())&&"TRUE".equals(output.getESB_FLAG())) {
					System.out.println("查询报账单状态接口返回数组长度"+collection.length);
					int j = 0;
					for(com.cmcc.soa.OSB_RBS_CMF_HQ_InquiryClaimDocStatusSrv.OUTPUTCOLLECTION_ITEM col : collection){
						j++;
						System.out.println("****************************第"+j+"条查询结果实体信息***********************************");
						System.out.println("源系统报账单编号【"+col.getSOURCE_DOCUMENT_NUM()+"】");
						System.out.println("报账系统报账单编号【"+col.getCLAIM_NUM()+"】");
						System.out.println("报账状态【"+col.getSTATUS_CODE()+"】");
						System.out.println("流程状态【"+col.getFLOW_STATUS_CODE()+"】");
						System.out.println("ERP接口状态【"+col.getERP_STATUS_CODE()+"】");
						this.jdbcTemplate.execute("call P_AT_STATE_DEAL('"+billType+"','"+col.getSOURCE_DOCUMENT_NUM()+"','"+col.getSTATUS_CODE()+"','"+col.getFLOW_STATUS_CODE()+"','"+col.getERP_STATUS_CODE()+"','"+""+"','"+""+"')");
					}
				}else {
					System.out.println("打印ESB 侧执行结果:"+output.getESB_FLAG());
					System.out.println("打印ESB 侧错误代码:"+output.getESB_RETURN_CODE());
					System.out.println("打印系统返回消:息"+output.getESB_RETURN_MESSAGE());
					System.out.println("打印业务服务侧执行结果:"+output.getBIZ_SERVICE_FLAG());
					System.out.println("打印业务服务侧错误代码:"+output.getBIZ_RETURN_CODE());
					System.out.println("打印业务服务返回消息:"+output.getBIZ_RETURN_MESSAGE());
					map.put("success", false);
					map.put("message", "查询报账单状态信息失败！");
				}
				System.out.println("本次查询报账单状态接口执行完毕！！");
			}
			
			} catch (Exception e) {
				System.out.println("*****************************接口执行过程出错****************************");
				e.printStackTrace();
			}
	     }
		
	/**
	 * OSB_ERP_GL_HQ_00012_查询科目余额信息服务_V0.1(分页)
	 * @return
	 * @throws Exception
	 */
	@Override
	public void queryPageInquiryAccuntBalanceInfo() {
		System.out.println("*******查询科目余额信息服务开始执行********");
		try {
			com.oracle.xmlns.apps.cux.rest.PageInquiryAccuntBalanceSrv.process.InputParameters input = new com.oracle.xmlns.apps.cux.rest.PageInquiryAccuntBalanceSrv.process.InputParameters();
			com.oracle.xmlns.apps.cux.rest.PageInquiryAccuntBalanceSrv.process.PROCESS_Input process_Input = new com.oracle.xmlns.apps.cux.rest.PageInquiryAccuntBalanceSrv.process.PROCESS_Input();
			// 填充头信息
			com.cmcc.soa.MsgHeader.MSGHEADER msgHeader = ittokeninfoService.getSOAHeader();
			msgHeader.setPROVINCE_CODE(QueryInputParamCostant.PROVINCE_CODE);
			SimpleDateFormat sdFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			Calendar startDateCal = Calendar.getInstance();
			Calendar endDateCal = Calendar.getInstance();
			Calendar stopDateCal = Calendar.getInstance();
			Date startDate = sdFormat.parse("2019-03-31 12:00:00");
			Date stopDate = sdFormat.parse("2019-07-19 12:00:00");
			startDateCal.setTime(startDate);
			endDateCal.setTime(startDate);
			stopDateCal.setTime(stopDate);
			endDateCal.add(Calendar.HOUR_OF_DAY, 12);
			do {
				input.setLAST_UPDATE_START(startDateCal);
				System.out.println("查询开始时间:" + sdFormat.format(startDateCal.getTime()));
				input.setLAST_UPDATE_END(endDateCal);
				System.out.println("查询结束时间:" + sdFormat.format(endDateCal.getTime()));
				// 必须有初始的更新查询时间
				int totalPage = 0;// 初始总页数为0
				BigDecimal headerPageSize = BigDecimal.valueOf(200);// 每页大小
				int headerCurrentPage = 1;// 当前页
				BigDecimal headerTotalRecord = BigDecimal.valueOf(-1);// 总记录数，第一次调用是传入-1
				com.oracle.xmlns.apps.cux.rest.PageInquiryAccuntBalanceSrv.process.OutputParameters output = null;
				String ESBCode = "ESB-01";
				for (int j = 0; j < 3; j++) {
					if ("ESB-01".equalsIgnoreCase(ESBCode)) {
						do {
							msgHeader.setPAGE_SIZE(headerPageSize);
							msgHeader.setCURRENT_PAGE(BigDecimal.valueOf(headerCurrentPage));
							msgHeader.setTOTAL_RECORD(headerTotalRecord);
							input.setMSGHEADER(msgHeader);
							process_Input.setInputParameters(input);
							output = WSCostUtil.processPageInquiryAccuntBalanceSrv(process_Input);
							System.out.println("打印ESB 侧执行结果:" + output.getESB_FLAG());
							System.out.println("打印ESB 侧错误代码:" + output.getESB_RETURN_CODE());
							System.out.println("打印系统返回消:息" + output.getESB_RETURN_MESSAGE());
							System.out.println("打印业务服务侧执行结果:" + output.getBIZ_SERVICE_FLAG());
							System.out.println("打印业务服务侧错误代码:" + output.getBIZ_RETURN_CODE());
							System.out.println("打印业务服务返回消息:" + output.getBIZ_RETURN_MESSAGE());
							ESBCode = output.getESB_RETURN_CODE() != null ? output.getESB_RETURN_CODE() : "";
							System.out.println("总页数***************" + output.getTOTAL_PAGE());
							if (output.getTOTAL_PAGE() != null) {
								totalPage = output.getTOTAL_PAGE().intValue();
							}
							com.oracle.xmlns.apps.cux.rest.PageInquiryAccuntBalanceSrv.process.OUTPUTCOLLECTION_ITEM[] outputCollection = output
									.getOUTPUTCOLLECTION();
							System.out.println("查询科目余额返回结果数组长度" + outputCollection.length);
							
							//手动控制事务
							DefaultTransactionDefinition def = new DefaultTransactionDefinition();
							def.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);// 事物隔离级别，开启新事务
						    TransactionStatus status = txManager.getTransaction(def); // 获得事务状态
							try {
								 for (com.oracle.xmlns.apps.cux.rest.PageInquiryAccuntBalanceSrv.process.OUTPUTCOLLECTION_ITEM outputcollection_ITEM : outputCollection) {
										IGrpErpSbjectBal igrperpsbjectbal = new IGrpErpSbjectBal();
										igrperpsbjectbal = WSCostUtilToll.switchBusinessEntity2WSEntity(outputcollection_ITEM,igrperpsbjectbal.getClass());
										int resultNum = igrperpsbjectbalDao.selectByKeyMap(igrperpsbjectbal);
										if (resultNum > 0) {
											igrperpsbjectbalDao.update(igrperpsbjectbal);
										} else {
											igrperpsbjectbalDao.create(igrperpsbjectbal);
										}
									}
								 txManager.commit(status);
								
							} catch (Exception e) {
								e.printStackTrace();
								System.out.println("保存科目余额信息出错！");
								txManager.rollback(status);
							}
							
							headerCurrentPage++;
						} while (headerCurrentPage <= totalPage);
						System.out.println("本次查询执行完毕！！！");
					}
				}
				System.out.println("当前时间：" + sdFormat.format(new Date()));
				startDateCal.add(Calendar.HOUR_OF_DAY, 12);
				endDateCal.add(Calendar.HOUR_OF_DAY, 12);
			} while (startDateCal.before(Calendar.getInstance()));
		} catch (Exception e) {
			System.out.println("*******查询科目余额信息服务_V0.1(分页)--执行出错********");
			e.printStackTrace();
		}
	}
	
	/**
	 * OSB_RBS_CMF_HQ_00043_查询业务大小类与业务活动映射信息服务（分页）
	 * 
	 */
	@Override
	public void testBizActivSrv() {
		System.out.println("进入到查询业务大小类信息服务接口方法");
		try {
			com.cmcc.soa.OSB_RBS_CMF_HQ_PageInquiryBizActivityMapSrv.InputParameters input=new com.cmcc.soa.OSB_RBS_CMF_HQ_PageInquiryBizActivityMapSrv.InputParameters();
			com.cmcc.soa.MsgHeader.MSGHEADER msgHeader = ittokeninfoService.getSOAHeader();
			msgHeader.setPROVINCE_CODE(QueryInputParamCostant.PROVINCE_CODE);
		    //set查询条件 
			input.setPROVINCE_CODE(QueryInputParamCostant.PROVINCE_CODE);
			input.setPROVINCE_NAME(QueryInputParamCostant.PROVINCE_NAME);
			//增量查询时放开
			//input.setLAST_UPDATE_START(WSCostUtilToll.getStartTime());
		    //input.setLAST_UPDATE_END(WSCostUtilToll.getEndTime());
			int totalPage=0;
			BigDecimal headerPageSize=BigDecimal.valueOf(100);//每页大小
			int headerCurrentPage=1;//当前页
			BigDecimal headerTotalRecord=BigDecimal.valueOf(-1);//总记录数，第一次调用是传入-1
			com.cmcc.soa.OSB_RBS_CMF_HQ_PageInquiryBizActivityMapSrv.OutputParameters output =null;
			String esbCode= "ESB-01";
				for(int j=0;j<3;j++){
					if("ESB-01".equalsIgnoreCase(esbCode)){
						do{
							msgHeader.setPAGE_SIZE(headerPageSize);
							msgHeader.setCURRENT_PAGE(BigDecimal.valueOf(headerCurrentPage));
							msgHeader.setTOTAL_RECORD(headerTotalRecord);
							input.setMSGHEADER(msgHeader);	
							output = WSCostUtil.processPageInquiryBizActivityMapSrv(input);
							System.out.println("打印ESB 侧执行结果:"+output.getESB_FLAG());
							System.out.println("打印ESB 侧错误代码:"+output.getESB_RETURN_CODE());
							System.out.println("打印系统返回消息"+output.getESB_RETURN_MESSAGE());
							System.out.println("打印业务服务侧执行结果:"+output.getBIZ_SERVICE_FLAG());
							System.out.println("打印业务服务侧错误代码:"+output.getBIZ_RETURN_CODE());
							System.out.println("打印业务服务返回消息:"+output.getBIZ_RETURN_MESSAGE());
							esbCode = output.getESB_RETURN_CODE()!=null?output.getESB_RETURN_CODE():"";
							System.out.println("总页数***************"+output.getTOTAL_PAGE());
							if(output.getTOTAL_PAGE()!=null) {
								totalPage=output.getTOTAL_PAGE().intValue();
							}
							com.cmcc.soa.OSB_RBS_CMF_HQ_PageInquiryBizActivityMapSrv.OUTPUTCOLLECTION_ITEM[] outputCollection=
									output.getOUTPUTCOLLECTION();
							System.out.println("查询业务大小类返回结果数组长度"+outputCollection.length);
							for(com.cmcc.soa.OSB_RBS_CMF_HQ_PageInquiryBizActivityMapSrv.OUTPUTCOLLECTION_ITEM coaItem:outputCollection){
								IGrpAtBnsType igrpatbnstype=new IGrpAtBnsType();
								igrpatbnstype = WSCostUtilToll.switchBusinessEntity2WSEntity(coaItem,igrpatbnstype.getClass());
								igrpatbnstype.setId(coaItem.getFLEX_ID());
								StringBuffer selectByFlexIdSql=new StringBuffer();
								selectByFlexIdSql.append("select count(*) from i_grp_at_bns_type where id='");
								selectByFlexIdSql.append(coaItem.getFLEX_ID());
								selectByFlexIdSql.append("'");
								int countNum=this.jdbcTemplate.queryForInt(selectByFlexIdSql.toString());
								if(countNum>0) {
									System.out.println("进入业务大小类更新方法");
									igrpatbnstypeDao.update(igrpatbnstype);
								}else {
									System.out.println("进入业务大小类新增方法");
									igrpatbnstypeDao.create(igrpatbnstype);
								}
							}
							headerCurrentPage++;
						}while(headerCurrentPage<=totalPage);
						System.out.println("本次查询执行完毕！！！");
					}else{
						break;
					}
		     }
		} catch (Exception e) {
			e.printStackTrace();
		}
    }
	
	
	/**
	 * OSB_ERP_AP_HQ_00017_查询应付发票凭证信息服务（分页）-因为ERP返回信息为发票维度不能对应到报账行信息上，故此接口弃用。
	 * 
	 */
	@Override
	public void queryApInvoiceVoucherSrv(String billType) {
		System.out.println("***********查询应付发票凭证信息服务（分页）**********");
		try {
			StringBuffer selectSql=new StringBuffer();
			StringBuffer updateSql=new StringBuffer();
			switch (billType) {
			case "AT":
				selectSql.append("select distinct a.finance_no,a.wf_no from t_wf_bns_at_main a left join t_wf_bns_at_line b on a.wf_no=b.wf_no");
				selectSql.append(" where a.push_state='成功' and nvl(a.at_state,'NEW') = 'COMPLETED' and a.finance_no is not null ");
				selectSql.append("and (b.subject_d is null  or b.subject_d_sub is null)");
				updateSql.append("update t_wf_bns_at_line set subject_d=?,subject_d_sub=? where wf_no=? and pri_key=?");
				break;
			case "WD":
				selectSql.append("select distinct a.finance_no,a.wf_no from t_wf_bns_wd_main a left join t_wf_bns_wd_line b on a.wf_no=b.wf_no");
				selectSql.append(" where a.push_state='成功' and nvl(a.wd_state,'NEW') = 'COMPLETED' and a.finance_no is not null ");
				selectSql.append("and (b.subject_d is null  or b.subject_d_sub is null)");
				updateSql.append("update t_wf_bns_wd_line set subject_d=?,subject_d_sub=? where wf_no=? and pri_key=?");
				break;
			case "WO":
				selectSql.append("select distinct a.finance_no,a.wf_no from t_wf_bns_wo_main a left join t_wf_bns_wo_line b on a.wf_no=b.wf_no");
				selectSql.append(" where a.push_state='成功' and nvl(a.wo_state,'NEW') = 'COMPLETED' and a.finance_no is not null  ");
				selectSql.append("and (b.subject_d is null or b.subject_d_sub is null)");
				updateSql.append("update t_wf_bns_wo_line set subject_d=?,subject_d_sub=? where wf_no=? and pri_key=?");
				break;
			default:
				break;
			}
			//获取数据
			List<Map<String, Object>> subjectList=this.jdbcTemplate.queryForList(selectSql.toString());
			System.out.println("本次循环查询的单据数量为："+subjectList.size());
			
			for (Map<String, Object> map : subjectList) {
				String wfNo=map.get("WF_NO")+"";
				String financeNo= map.get("FINANCE_NO")+"";
				com.oracle.xmlns.apps.cux.rest.PageInquiryApInvoiceVoucherSrv.process.InputParameters input = new com.oracle.xmlns.apps.cux.rest.PageInquiryApInvoiceVoucherSrv.process.InputParameters();
				com.oracle.xmlns.apps.cux.rest.PageInquiryApInvoiceVoucherSrv.process.PROCESS_Input process_Input = new com.oracle.xmlns.apps.cux.rest.PageInquiryApInvoiceVoucherSrv.process.PROCESS_Input();
				//填充头信息
				com.cmcc.soa.MsgHeader.MSGHEADER msgHeader = ittokeninfoService.getSOAHeader();
				msgHeader.setPROVINCE_CODE(QueryInputParamCostant.PROVINCE_CODE);
				input.setMSGHEADER(msgHeader);
				input.setSOURCE_DOCUMENT_NUM(financeNo);
				process_Input.setInputParameters(input);
				com.oracle.xmlns.apps.cux.rest.PageInquiryApInvoiceVoucherSrv.process.OutputParameters output = 
						WSCostUtil.processPageInquiryApInvoiceVoucherSrv(process_Input);
				if ("TRUE".equals(output.getBIZ_SERVICE_FLAG())&&"TRUE".equals(output.getESB_FLAG())) {
					com.oracle.xmlns.apps.cux.rest.PageInquiryApInvoiceVoucherSrv.process.OUTPUTCOLLECTION_ITEM[] collection=output.getOUTPUTCOLLECTION();
					System.out.println("查询应付发票凭证信息服务接口返回数组长度"+collection.length);
					int j = 0;
					for(com.oracle.xmlns.apps.cux.rest.PageInquiryApInvoiceVoucherSrv.process.OUTPUTCOLLECTION_ITEM col : collection){
						j++;
						System.out.println("****************************第"+j+"条查询结果实体信息***********************************");
						System.out.println("源系统报账单编号(报账平台)【"+col.getSOURCE_DOCUMENT_NUM()+"】");
						System.out.println("子分类账账户组合【"+col.getACCOUNT()+"】");
						System.out.println("凭证行号【"+col.getLINE_NUMBER()+"】");
//						int Num=this.jdbcTemplate.update(updateSql.toString(),"","","","","");
//						System.out.println("成功更新"+Num+"条账单行信息！");
					}
				}else {
					System.out.println("打印ESB 侧执行结果:"+output.getESB_FLAG());
					System.out.println("打印ESB 侧错误代码:"+output.getESB_RETURN_CODE());
					System.out.println("打印系统返回消:息"+output.getESB_RETURN_MESSAGE());
					System.out.println("打印业务服务侧执行结果:"+output.getBIZ_SERVICE_FLAG());
					System.out.println("打印业务服务侧错误代码:"+output.getBIZ_RETURN_CODE());
					System.out.println("打印业务服务返回消息:"+output.getBIZ_RETURN_MESSAGE());
					map.put("success", false);
					map.put("message", "查询应付发票凭证信息服务失败！");
				}
				System.out.println("查询应付发票凭证信息服务接口执行完毕！！");
			}
		} catch (Exception e) {
			System.out.println("*****************************接口执行过程出错****************************");
			e.printStackTrace();
		}
     }
	
	/**
	 * OSB_ERP_GL_HQ_00016_查询会计科目组合信息服务（分页）
	 * 暂未使用
	 */
	@Override
	public void queryPageInquiryAccuntInfoCodeCombInfo() {
		try {
			com.oracle.xmlns.apps.cux.rest.PageInquiryAccountCodeCombSrv.process.InputParameters input = new com.oracle.xmlns.apps.cux.rest.PageInquiryAccountCodeCombSrv.process.InputParameters();
			com.oracle.xmlns.apps.cux.rest.PageInquiryAccountCodeCombSrv.process.PROCESS_Input process_Input = new com.oracle.xmlns.apps.cux.rest.PageInquiryAccountCodeCombSrv.process.PROCESS_Input();
			//填充头信息
			com.cmcc.soa.MsgHeader.MSGHEADER msgHeader = ittokeninfoService.getSOAHeader();
			msgHeader.setPROVINCE_CODE(QueryInputParamCostant.PROVINCE_CODE);
			//input.setLAST_UPDATE_START(WSCostUtilToll.getStartTimeCal());
		    //input.setLAST_UPDATE_END(WSCostUtilToll.getEndTimeCal());
			int totalPage=0;//初始总页数为0
			BigDecimal headerPageSize=BigDecimal.valueOf(200);//每页大小
			int headerCurrentPage=1;//当前页
			BigDecimal headerTotalRecord=BigDecimal.valueOf(-1);//总记录数，第一次调用是传入-1
			com.oracle.xmlns.apps.cux.rest.PageInquiryAccountCodeCombSrv.process.OutputParameters output=null;
			String ESBCode= "ESB-01";
				for(int j=0;j<3;j++){
					if("ESB-01".equalsIgnoreCase(ESBCode)){
						do{
							msgHeader.setPAGE_SIZE(headerPageSize);
							msgHeader.setCURRENT_PAGE(BigDecimal.valueOf(headerCurrentPage));
							msgHeader.setTOTAL_RECORD(headerTotalRecord);
							input.setMSGHEADER(msgHeader);
							process_Input.setInputParameters(input);
							output = WSCostUtil.processPageInquiryAccountCodeCombSrv(process_Input);
							System.out.println("打印ESB 侧执行结果:"+output.getESB_FLAG());
							System.out.println("打印ESB 侧错误代码:"+output.getESB_RETURN_CODE());
							System.out.println("打印系统返回消息"+output.getESB_RETURN_MESSAGE());
							System.out.println("打印业务服务侧执行结果:"+output.getBIZ_SERVICE_FLAG());
							System.out.println("打印业务服务侧错误代码:"+output.getBIZ_RETURN_CODE());
							System.out.println("打印业务服务返回消息:"+output.getBIZ_RETURN_MESSAGE());
							ESBCode = output.getESB_RETURN_CODE()!=null?output.getESB_RETURN_CODE():"";
							System.out.println("总页数***************"+output.getTOTAL_PAGE());
							if(output.getTOTAL_PAGE()!=null) {
								totalPage=output.getTOTAL_PAGE().intValue();
							}
							com.oracle.xmlns.apps.cux.rest.PageInquiryAccountCodeCombSrv.process.OUTPUTCOLLECTION_ITEM[] outputCollection=
									output.getOUTPUTCOLLECTION();
							System.out.println("查询会计科目组合返回结果数组长度"+outputCollection.length);
							for (com.oracle.xmlns.apps.cux.rest.PageInquiryAccountCodeCombSrv.process.OUTPUTCOLLECTION_ITEM outputcollection_ITEM : outputCollection) {
								IGrpErpSbject igrperpsbject=new IGrpErpSbject();
								igrperpsbject=WSCostUtilToll.switchBusinessEntity2WSEntity(outputcollection_ITEM,igrperpsbject.getClass());
								StringBuffer selectByCodeCombinationIdSql=new StringBuffer();
								selectByCodeCombinationIdSql.append("select count(1) from i_grp_erp_sbject where CODE_COMBINATION_ID =?");
								int countNum=this.jdbcTemplate.queryForInt(selectByCodeCombinationIdSql.toString(),outputcollection_ITEM.getCODE_COMBINATION_ID());
								if(countNum>0) {
									igrperpsbjectDao.update(igrperpsbject);
								}else {
									igrperpsbjectDao.create(igrperpsbject);
								}
							}
							headerCurrentPage++;
						}while(headerCurrentPage<=totalPage);
						System.out.println("本次查询执行完毕！！！");
					}else{
						break;
					}
		      }
		} catch (Exception e) {
			System.out.println("*******查询会计科目组合信息服务(分页)--执行出错********");
			e.printStackTrace();
		}
	}
	
	/**
	 * OSB_ERP_GL_HQ_00009_查询OU组织信息服务_V0.1
	 * 
	 */
	@Override
	public void processInquiryOUOrganizationInfo() {
		try {
			com.oracle.xmlns.apps.cux.rest.InquiryOUOrganizationSrv.process.InputParameters input = new com.oracle.xmlns.apps.cux.rest.InquiryOUOrganizationSrv.process.InputParameters();
			com.oracle.xmlns.apps.cux.rest.InquiryOUOrganizationSrv.process.PROCESS_Input process_Input = new com.oracle.xmlns.apps.cux.rest.InquiryOUOrganizationSrv.process.PROCESS_Input();
			//填充头信息
			com.cmcc.soa.MsgHeader.MSGHEADER msgHeader = ittokeninfoService.getSOAHeader();
			msgHeader.setPROVINCE_CODE(QueryInputParamCostant.PROVINCE_CODE);
			input.setMSGHEADER(msgHeader);
			//增量查询时放开
			//input.setLAST_UPDATE_START(WSCostUtilToll.getStartTimeCal());
		    //input.setLAST_UPDATE_END(WSCostUtilToll.getEndTimeCal());
			process_Input.setInputParameters(input);
			com.oracle.xmlns.apps.cux.rest.InquiryOUOrganizationSrv.process.OutputParameters output = WSCostUtil.processInquiryOUOrganizationSrv(process_Input);
			
			if ("TRUE".equals(output.getBIZ_SERVICE_FLAG())&&"TRUE".equals(output.getESB_FLAG())) {
				com.oracle.xmlns.apps.cux.rest.InquiryOUOrganizationSrv.process.OUTPUTCOLLECTION_ITEM[] collection = output.getOUTPUTCOLLECTION();
				System.out.println("查询OU组织信息服务接口返回数组长度"+collection.length);
				System.out.println("********************开始循环打印OU组织数据实体*******************");
				int m = 0;
				for(com.oracle.xmlns.apps.cux.rest.InquiryOUOrganizationSrv.process.OUTPUTCOLLECTION_ITEM col : collection){
					m++;
					System.out.println("*********************第"+m+"条OU组织数据******************");
					Class cls = col.getClass();
					Field[] fields = cls.getDeclaredFields();  
					for(int n=0; n<fields.length; n++){  
						Field f = fields[n];  
						f.setAccessible(true);  
						System.out.println("属性名:" + f.getName() + " 属性值:" + f.get(col));  
					}
					//数据入库
					IGrpErpOu igrperpou=new IGrpErpOu();
					igrperpou=WSCostUtilToll.switchBusinessEntity2WSEntity(col, igrperpou.getClass());
					int countNum=igrperpouDao.selectByOrgId(col.getORG_ID().toString());
					if(countNum>0) {
						igrperpouDao.update(igrperpou);
					}else {
						igrperpouDao.create(igrperpou);
					}
				}
				System.out.println("********************循环打印OU组织数据实体结束*******************");
			}else {
				System.out.println("打印ESB 侧执行结果:"+output.getESB_FLAG());
				System.out.println("打印ESB 侧错误代码:"+output.getESB_RETURN_CODE());
				System.out.println("打印系统返回消息"+output.getESB_RETURN_MESSAGE());
				System.out.println("打印业务服务侧执行结果:"+output.getBIZ_SERVICE_FLAG());
				System.out.println("打印业务服务侧错误代码:"+output.getBIZ_RETURN_CODE());
				System.out.println("打印业务服务返回消息:"+output.getBIZ_RETURN_MESSAGE());
			}
		} catch (Exception e) {
			System.out.println("查询OU组织信息服务接口出错！");
			e.printStackTrace();
		}
	}
	
	/**
	 * 查询资产头基本信息服务（分页）
	 * 定时任务增量查询
	 */
	@Override
	public void queryAssetHeadJob(){
		try {
			com.oracle.xmlns.apps.cux.rest.PageInquiryAssetHeadSrv.process.InputParameters input = new com.oracle.xmlns.apps.cux.rest.PageInquiryAssetHeadSrv.process.InputParameters();
			com.oracle.xmlns.apps.cux.rest.PageInquiryAssetHeadSrv.process.PROCESS_Input process_input = new com.oracle.xmlns.apps.cux.rest.PageInquiryAssetHeadSrv.process.PROCESS_Input();
			// 填充头信息
			com.cmcc.soa.MsgHeader.MSGHEADER msgHeader = ittokeninfoService.getSOAHeader();
			msgHeader.setPROVINCE_CODE(QueryInputParamCostant.PROVINCE_CODE);
			msgHeader.setROUTE_CODE(QueryInputParamCostant.ROUTE_CODE);
			input.setMSGHEADER(msgHeader);
			//增量查询
			input.setLAST_UPDATE_START(WSCostUtilToll.getStartTimeCal());
			input.setLAST_UPDATE_END(WSCostUtilToll.getEndTimeCal());
			BigDecimal headerPageSize = BigDecimal.valueOf(200);// 每页大小
			int currentPage = 1;// 当前页
			int totalPage = -1;// 总页数
			String esbCode = "ESB-01";
			for (int j = 0; j < 3; j++) {
				if ("ESB-01".equalsIgnoreCase(esbCode)) {
					do {
						msgHeader.setPAGE_SIZE(headerPageSize);
						msgHeader.setCURRENT_PAGE(BigDecimal.valueOf(currentPage));
						msgHeader.setTOTAL_RECORD(BigDecimal.valueOf(totalPage));
						input.setMSGHEADER(msgHeader);
						process_input.setInputParameters(input);
						com.oracle.xmlns.apps.cux.rest.PageInquiryAssetHeadSrv.process.OutputParameters output = WSCostUtil
								.processPageInquiryAssetHeadSrv(process_input);
						esbCode = output.getESB_RETURN_CODE() != null ? output.getESB_RETURN_CODE() : "";
						System.out.println("打印ESB侧执行结果:" + output.getESB_FLAG());
						System.out.println("打印BIZ侧执行结果:" + output.getBIZ_SERVICE_FLAG());
						if ("TRUE".equals(output.getBIZ_SERVICE_FLAG()) && "TRUE".equals(output.getESB_FLAG())) {
							System.out.println("********************总页数***************" + output.getTOTAL_PAGE());
							System.out.println("********************当前页***************" + currentPage);
							com.oracle.xmlns.apps.cux.rest.PageInquiryAssetHeadSrv.process.OUTPUTCOLLECTION_ITEM[] outputCollection = output
									.getOUTPUTCOLLECTION();
							try {
								for (com.oracle.xmlns.apps.cux.rest.PageInquiryAssetHeadSrv.process.OUTPUTCOLLECTION_ITEM col : outputCollection) {
									IGrpErpZc igrperpzc = new IGrpErpZc();
									igrperpzc = WSCostUtilToll.switchBusinessEntity2WSEntity(col, igrperpzc.getClass());
									igrperpzcDao.createOrUpdate(igrperpzc);
								}
							} catch (Exception e) {
								e.printStackTrace();
								System.out.println("资产头信息数据入库错误");
							}
						} else {
							System.out.println("打印ESB侧错误代码:" + output.getESB_RETURN_CODE());
							System.out.println("打印系统返回消息" + output.getESB_RETURN_MESSAGE());
							System.out.println("打印BIZ侧错误代码:" + output.getBIZ_RETURN_CODE());
							System.out.println("打印BIZ返回消息:" + output.getBIZ_RETURN_MESSAGE());
						}
						if (output.getTOTAL_PAGE() != null) {
							totalPage = output.getTOTAL_PAGE().intValue();
						}
						currentPage++;
					} while (currentPage <= totalPage);
					System.out.println("本次查询执行完毕！！！");
				};
			}
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("资产头信息全量查询出错！");
		}
	}
	
	/**
	 * 查询预算项目信息服务
	 * 定时增量查询
	 */
	@Override
	public void queryGrpBgtProjectJob() {
		try {
			int totalPage=0;
			BigDecimal headerPageSize=BigDecimal.valueOf(100);//每页大小
			int headerCurrentPage=1;//当前页
			BigDecimal headerTotalRecord=BigDecimal.valueOf(-1);//总页数
			com.cmcc.soa.MsgHeader.MSGHEADER msgHeader = ittokeninfoService.getSOAHeader();
			msgHeader.setPROVINCE_CODE(QueryInputParamCostant.PROVINCE_CODE);
			msgHeader.setROUTE_CODE(QueryInputParamCostant.ROUTE_CODE);
			//定义查询输入输出
			com.cmcc.soa.OSB_BP_SOA_HQ_PageInquiryBudgetProjRouteSrv.OutputParameters output =null;
			com.cmcc.soa.OSB_BP_SOA_HQ_PageInquiryBudgetProjRouteSrv.InputParameters input=new com.cmcc.soa.OSB_BP_SOA_HQ_PageInquiryBudgetProjRouteSrv.InputParameters();
			//增量查询
			input.setLAST_UPDATE_START(WSCostUtilToll.getStartTimeCal());
			input.setLAST_UPDATE_END(WSCostUtilToll.getEndTimeCal());
			do{
				msgHeader.setPAGE_SIZE(headerPageSize);
				msgHeader.setCURRENT_PAGE(BigDecimal.valueOf(headerCurrentPage));
				msgHeader.setTOTAL_RECORD(headerTotalRecord);
				input.setMSGHEADER(msgHeader);
				output=WSCostUtil.processPageInquiryBudgetProjRouteSrv(input);
				//打印执行结果日志
				System.out.println("查询预算项目信息服务+打印ESB 侧执行结果:"+output.getESB_FLAG());
				System.out.println("打印ESB 侧错误代码:"+output.getESB_RETURN_CODE());
				System.out.println("打印系统返回消息:"+output.getESB_RETURN_MESSAGE());
				System.out.println("打印业务服务侧执行结果:"+output.getBIZ_SERVICE_FLAG());
				System.out.println("打印业务服务侧错误代码:"+output.getBIZ_RETURN_CODE());
				System.out.println("打印业务服务返回消息:"+output.getBIZ_RETURN_MESSAGE());
				com.cmcc.soa.OSB_BP_SOA_HQ_PageInquiryBudgetProjRouteSrv.OUTPUTCOLLECTION_ITEM[] collection=output.getOUTPUTCOLLECTION();
				System.out.println("查询预算信息服务返回数组长度"+collection.length);
				System.out.println("总页数***************"+output.getTOTAL_PAGE());
				if(output.getTOTAL_PAGE()!=null) {
					totalPage=output.getTOTAL_PAGE().intValue();
				}
				
				com.cmcc.soa.OSB_BP_SOA_HQ_PageInquiryBudgetProjRouteSrv.OUTPUTCOLLECTION_ITEM[] outputCollection=output.getOUTPUTCOLLECTION();
			    int i=0;
				for (com.cmcc.soa.OSB_BP_SOA_HQ_PageInquiryBudgetProjRouteSrv.OUTPUTCOLLECTION_ITEM outputcollection_ITEM : outputCollection) {
			    	i++;
			    	IGrpBgtProject igrpbgtproject=new IGrpBgtProject();
			    	igrpbgtproject=WSCostUtilToll.switchBusinessEntity2WSEntity(outputcollection_ITEM,igrpbgtproject.getClass() );
			    	Calendar startDate=outputcollection_ITEM.getSTART_DATE();
			    	Calendar endDate=outputcollection_ITEM.getEND_DATE();
			    	Calendar lastUpdateDate=outputcollection_ITEM.getLAST_UPDATE_DATE();
			    	if(startDate!=null){
			    		igrpbgtproject.setStartDate(startDate.getTime());
			    	}
			    	if(endDate!=null){
			    		igrpbgtproject.setEndDate(endDate.getTime());
			    	}
			    	if(lastUpdateDate!=null){
			    		igrpbgtproject.setLastUpdateDate(lastUpdateDate.getTime());
			    	}
			    	
			    	try {
			    		igrpbgtprojectDao.createOrUpdate(igrpbgtproject);
					} catch (Exception e) {
						e.printStackTrace();
						System.out.println("保存预算项目信息数据出错");
					}
			    }
				headerCurrentPage++;
			}while(headerCurrentPage<=totalPage);
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * 查询预算项目与报账部门映射关系信息服务
	 */
	@Override
	public void queryGrpBgtProjectOrgRelaJob() {
		try {
			int totalPage=0;
			BigDecimal headerPageSize=BigDecimal.valueOf(100);//每页大小
			int headerCurrentPage=1;//当前页
			BigDecimal headerTotalRecord=BigDecimal.valueOf(-1);//总页数
			com.cmcc.soa.MsgHeader.MSGHEADER msgHeader = ittokeninfoService.getSOAHeader();
			msgHeader.setPROVINCE_CODE(QueryInputParamCostant.PROVINCE_CODE);
			msgHeader.setROUTE_CODE(QueryInputParamCostant.ROUTE_CODE);
			//定义查询输入输出
			com.cmcc.soa.OSB_BP_SOA_HQ_PageInquiryProjMapDeptRouteSrv.OutputParameters output =null;
			com.cmcc.soa.OSB_BP_SOA_HQ_PageInquiryProjMapDeptRouteSrv.InputParameters input=new com.cmcc.soa.OSB_BP_SOA_HQ_PageInquiryProjMapDeptRouteSrv.InputParameters();
			//增量查询
			input.setLAST_UPDATE_START(WSCostUtilToll.getStartTimeCal());
			input.setLAST_UPDATE_END(WSCostUtilToll.getEndTimeCal());
			do{
				msgHeader.setPAGE_SIZE(headerPageSize);
				msgHeader.setCURRENT_PAGE(BigDecimal.valueOf(headerCurrentPage));
				msgHeader.setTOTAL_RECORD(headerTotalRecord);
				input.setMSGHEADER(msgHeader);
				output=WSCostUtil.processPageInquiryProjMapDeptRouteSrv(input);
				//打印执行结果日志
				System.out.println("查询预算项目与报账部门映射关系信息服务+打印ESB 侧执行结果:"+output.getESB_FLAG());
				System.out.println("打印ESB 侧错误代码:"+output.getESB_RETURN_CODE());
				System.out.println("打印系统返回消息:"+output.getESB_RETURN_MESSAGE());
				System.out.println("打印业务服务侧执行结果:"+output.getBIZ_SERVICE_FLAG());
				System.out.println("打印业务服务侧错误代码:"+output.getBIZ_RETURN_CODE());
				System.out.println("打印业务服务返回消息:"+output.getBIZ_RETURN_MESSAGE());
				com.cmcc.soa.OSB_BP_SOA_HQ_PageInquiryProjMapDeptRouteSrv.OUTPUTCOLLECTION_ITEM[] collection=output.getOUTPUTCOLLECTION();
				System.out.println("查询预算信息服务返回数组长度"+collection.length);
				System.out.println("总页数***************"+output.getTOTAL_PAGE());
				if(output.getTOTAL_PAGE()!=null) {
					totalPage=output.getTOTAL_PAGE().intValue();
				}
				com.cmcc.soa.OSB_BP_SOA_HQ_PageInquiryProjMapDeptRouteSrv.OUTPUTCOLLECTION_ITEM[] outputCollection=output.getOUTPUTCOLLECTION();
			    int i=0;
				for (com.cmcc.soa.OSB_BP_SOA_HQ_PageInquiryProjMapDeptRouteSrv.OUTPUTCOLLECTION_ITEM outputcollection_ITEM : outputCollection) {
			    	i++;
			    	Calendar cal1=outputcollection_ITEM.getSTART_DATE();
			    	Date d1=cal1.getTime();
			    	Calendar cal2=outputcollection_ITEM.getEND_DATE();
			    	Date d2=cal2.getTime();
			    	Calendar cal3=outputcollection_ITEM.getLAST_UPDATE_DATE();
			    	Date d3=cal3.getTime();
			    	IGrpBgtProjectOrgRela igrpbgtprojectorgrela=new IGrpBgtProjectOrgRela();
			    	igrpbgtprojectorgrela=WSCostUtilToll.switchBusinessEntity2WSEntity(outputcollection_ITEM,igrpbgtprojectorgrela.getClass() );
			    	igrpbgtprojectorgrela.setStartDate(d1);
			    	igrpbgtprojectorgrela.setEndDate(d2);
			    	igrpbgtprojectorgrela.setLastUpdateDate(d3);
			    	igrpbgtprojectorgrelaDao.createOrUpdate(igrpbgtprojectorgrela);
			    }
				
				headerCurrentPage++;
			}while(headerCurrentPage<=totalPage);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	
	/**
	 * 查询多维映射关系信息服务
	 */
	@Override
	public void queryGrpBgtMultiRelaJob() {
		try {
			int totalPage=0;
			BigDecimal headerPageSize=BigDecimal.valueOf(100);//每页大小
			int headerCurrentPage=1;//当前页
			BigDecimal headerTotalRecord=BigDecimal.valueOf(-1);//总页数
			com.cmcc.soa.MsgHeader.MSGHEADER msgHeader = ittokeninfoService.getSOAHeader();
			msgHeader.setPROVINCE_CODE(QueryInputParamCostant.PROVINCE_CODE);
			msgHeader.setROUTE_CODE(QueryInputParamCostant.ROUTE_CODE);
			//定义查询输入输出
			com.cmcc.soa.OSB_BP_SOA_HQ_PageInquiryMappingRouteSrv.OutputParameters output =null;
			com.cmcc.soa.OSB_BP_SOA_HQ_PageInquiryMappingRouteSrv.InputParameters input=new com.cmcc.soa.OSB_BP_SOA_HQ_PageInquiryMappingRouteSrv.InputParameters();
			//增量查询
			input.setLAST_UPDATE_START(WSCostUtilToll.getStartTimeCal());
			input.setLAST_UPDATE_END(WSCostUtilToll.getEndTimeCal());
			do{
				msgHeader.setPAGE_SIZE(headerPageSize);
				msgHeader.setCURRENT_PAGE(BigDecimal.valueOf(headerCurrentPage));
				msgHeader.setTOTAL_RECORD(headerTotalRecord);
				input.setMSGHEADER(msgHeader);
				output=WSCostUtil.processPageInquiryMappingRouteSrv(input);
				//打印执行结果日志
				System.out.println("查询多维映射关系信息服务+打印ESB 侧执行结果:"+output.getESB_FLAG());
				System.out.println("打印ESB 侧错误代码:"+output.getESB_RETURN_CODE());
				System.out.println("打印系统返回消息:"+output.getESB_RETURN_MESSAGE());
				System.out.println("打印业务服务侧执行结果:"+output.getBIZ_SERVICE_FLAG());
				System.out.println("打印业务服务侧错误代码:"+output.getBIZ_RETURN_CODE());
				System.out.println("打印业务服务返回消息:"+output.getBIZ_RETURN_MESSAGE());
				com.cmcc.soa.OSB_BP_SOA_HQ_PageInquiryMappingRouteSrv.OUTPUTCOLLECTION_ITEM[] collection=output.getOUTPUTCOLLECTION();
				System.out.println("查询预算信息服务返回数组长度"+collection.length);
				System.out.println("总页数***************"+output.getTOTAL_PAGE());
				if(output.getTOTAL_PAGE()!=null) {
					totalPage=output.getTOTAL_PAGE().intValue();
				}
				
				com.cmcc.soa.OSB_BP_SOA_HQ_PageInquiryMappingRouteSrv.OUTPUTCOLLECTION_ITEM[] outputCollection=output.getOUTPUTCOLLECTION();
			    int i=0;
				for (com.cmcc.soa.OSB_BP_SOA_HQ_PageInquiryMappingRouteSrv.OUTPUTCOLLECTION_ITEM outputcollection_ITEM : outputCollection) {
			    	i++;
			    	IGrpBgtMultiRela igrpbgtmultirela=new IGrpBgtMultiRela();
			    	igrpbgtmultirela=WSCostUtilToll.switchBusinessEntity2WSEntity(outputcollection_ITEM,igrpbgtmultirela.getClass() );
			    	Calendar startDate=outputcollection_ITEM.getSTART_DATE();
			    	Calendar endDate=outputcollection_ITEM.getEND_DATE();
			    	Calendar lastUpdateDate=outputcollection_ITEM.getLAST_UPDATE_DATE();
			    	if(startDate!=null){
			    		igrpbgtmultirela.setStartDate(startDate.getTime());
			    	}
			    	if(endDate!=null){
			    		igrpbgtmultirela.setEndDate(endDate.getTime());
			    	}
			    	if(lastUpdateDate!=null){
			    		igrpbgtmultirela.setLastUpdateDate(lastUpdateDate.getTime());
			    	}
			    	if(igrpbgtmultirela.getPriKey()!=null){
			    		igrpbgtmultirelaDao.createOrUpdate(igrpbgtmultirela);
			    	}
			    	
			    }
				
				headerCurrentPage++;
			}while(headerCurrentPage<=totalPage);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * 查询预算信息路由服务
	 */
	@Override
	public void queryGrpBgtBaseJob() {
		try {
			com.cmcc.soa.OSB_BP_SOA_HQ_InquiryBudgetRouteSrv.InputParameters input = new com.cmcc.soa.OSB_BP_SOA_HQ_InquiryBudgetRouteSrv.InputParameters();
			com.cmcc.soa.MsgHeader.MSGHEADER msgHeader = ittokeninfoService.getSOAHeader();
			msgHeader.setPROVINCE_CODE(QueryInputParamCostant.PROVINCE_CODE);
			msgHeader.setROUTE_CODE(QueryInputParamCostant.ROUTE_CODE);
			input.setMSGHEADER(msgHeader);
			//增量查询
			input.setLAST_UPDATE_START(WSCostUtilToll.getStartTimeCal());
			input.setLAST_UPDATE_END(WSCostUtilToll.getEndTimeCal());
			// 循环获取数据
			String sql = "select  ORGNO as orgno from I_UUM_ORG";
			List<String> orgnos = this.jdbcTemplate.queryForList(sql, String.class);
			for (String orgno : orgnos) {
				input.setBUDGET_DEPT_CODE(orgno);
				com.cmcc.soa.OSB_BP_SOA_HQ_InquiryBudgetRouteSrv.OutputParameters output = WSCostUtil
						.processPageInquiryBudgetRouteSrv(input);
				// 打印执行结果日志
				System.out.println("查询预算信息服务+打印ESB 侧执行结果:" + output.getESB_FLAG());
				System.out.println("打印ESB 侧错误代码:" + output.getESB_RETURN_CODE());
				System.out.println("打印系统返回消息:" + output.getESB_RETURN_MESSAGE());
				System.out.println("打印业务服务侧执行结果:" + output.getBIZ_SERVICE_FLAG());
				System.out.println("打印业务服务侧错误代码:" + output.getBIZ_RETURN_CODE());
				System.out.println("打印业务服务返回消息:" + output.getBIZ_RETURN_MESSAGE());
				com.cmcc.soa.OSB_BP_SOA_HQ_InquiryBudgetRouteSrv.OUTPUTCOLLECTION_ITEM[] collection = output
						.getOUTPUTCOLLECTION();
				System.out.println("查询预算信息路由服务返回数组长度" + collection.length);
				com.cmcc.soa.OSB_BP_SOA_HQ_InquiryBudgetRouteSrv.OUTPUTCOLLECTION_ITEM[] outputCollection = output
						.getOUTPUTCOLLECTION();
				int j = 0;
				for (com.cmcc.soa.OSB_BP_SOA_HQ_InquiryBudgetRouteSrv.OUTPUTCOLLECTION_ITEM outputcollection_ITEM : outputCollection) {
					j++;
					IGrpBgtBase igrpbgtbase = new IGrpBgtBase();
					igrpbgtbase = WSCostUtilToll.switchBusinessEntity2WSEntity(outputcollection_ITEM,
							igrpbgtbase.getClass());
					Calendar lastUpdateDate = outputcollection_ITEM.getLAST_UPDATE_DATE();
					if (lastUpdateDate != null) {
						igrpbgtbase.setLastUpdateDate(lastUpdateDate.getTime());
					}
					try {
						igrpbgtbaseDao.createOrUpdate(igrpbgtbase);
					} catch (Exception e) {
						System.out.println("保存预算信息路由出错");
						e.printStackTrace();
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * 查询合同基本信息服务
	 */
	@Override
	public void queryGrpCtrBaseJob() {
		try {
			SimpleDateFormat sf = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
			int totalPage = 0;
			BigDecimal headerPageSize = BigDecimal.valueOf(100);// 每页大小
			int headerCurrentPage = 1;// 当前页
			BigDecimal headerTotalRecord = BigDecimal.valueOf(-1);// 总页数
			com.cmcc.soa.MsgHeader.MSGHEADER msgHeader = ittokeninfoService.getSOAHeader();
			msgHeader.setPROVINCE_CODE(QueryInputParamCostant.PROVINCE_CODE);
			// 定义查询输入输出
			com.cmcc.soa.OSB_CMS_CMS_HQ_PageInquiryContractBaseSrv.OutputParameters output = null;
			com.cmcc.soa.OSB_CMS_CMS_HQ_PageInquiryContractBaseSrv.InputParameters input = new com.cmcc.soa.OSB_CMS_CMS_HQ_PageInquiryContractBaseSrv.InputParameters();
			input.setPROVINCE_CODE(QueryInputParamCostant.PROVINCE_CODE);
			// 增量查询时放开
			input.setLAST_UPDATE_START(WSCostUtilToll.getTodayStartTimeCal());
			input.setLAST_UPDATE_END(WSCostUtilToll.getTodayEndTimeCal());
			do {
				msgHeader.setPAGE_SIZE(headerPageSize);
				msgHeader.setCURRENT_PAGE(BigDecimal.valueOf(headerCurrentPage));
				msgHeader.setTOTAL_RECORD(headerTotalRecord);
				input.setMSGHEADER(msgHeader);
				output = WSCostUtil.processPageInquiryContractBaseSrv(input);
				// 打印执行结果日志
				System.out.println("查询合同基本信息服务+打印ESB 侧执行结果:" + output.getESB_FLAG());
				System.out.println("打印ESB 侧错误代码:" + output.getESB_RETURN_CODE());
				System.out.println("打印系统返回消息:" + output.getESB_RETURN_MESSAGE());
				System.out.println("打印业务服务侧执行结果:" + output.getBIZ_SERVICE_FLAG());
				System.out.println("打印业务服务侧错误代码:" + output.getBIZ_RETURN_CODE());
				System.out.println("打印业务服务返回消息:" + output.getBIZ_RETURN_MESSAGE());
				System.out.println("总页数***************" + output.getTOTAL_PAGE());
				if (output.getTOTAL_PAGE() != null) {
					totalPage = output.getTOTAL_PAGE().intValue();
				}
				// 获取数据实体，保存记录
				com.cmcc.soa.OSB_CMS_CMS_HQ_PageInquiryContractBaseSrv.OUTPUTCOLLECTION_ITEM[] outputCollection = output
						.getOUTPUTCOLLECTION();
				System.out.println("查询合同基本信息数组长度：" + outputCollection.length);
				for (com.cmcc.soa.OSB_CMS_CMS_HQ_PageInquiryContractBaseSrv.OUTPUTCOLLECTION_ITEM outputcollection_ITEM : outputCollection) {
					IGrpCtrBase igrpctrbase = new IGrpCtrBase();
					igrpctrbase = WSCostUtilToll.switchBusinessEntity2WSEntity(outputcollection_ITEM,
							igrpctrbase.getClass());
					// 转化日期字段格式
					Calendar createDate = outputcollection_ITEM.getCREATE_DATE();
					Calendar approveDate = outputcollection_ITEM.getAPPROVE_DATE();
					Calendar signedDate = outputcollection_ITEM.getSIGNED_DATE();
					Calendar startDate = outputcollection_ITEM.getSTART_DATE();
					Calendar endDate = outputcollection_ITEM.getEND_DATE();
					Calendar lastUpdateDate = outputcollection_ITEM.getLAST_UPDATE_DATE();
					if (createDate != null) {
						igrpctrbase.setCreateDate(sf.format(createDate.getTime()));
					}
					if (approveDate != null) {
						igrpctrbase.setApproveDate(sf.format(approveDate.getTime()));
					}
					if (signedDate != null) {
						igrpctrbase.setSignedDate(sf.format(signedDate.getTime()));
					}
					if (startDate != null) {
						igrpctrbase.setApproveDate(sf.format(startDate.getTime()));
					}
					if (endDate != null) {
						igrpctrbase.setApproveDate(sf.format(endDate.getTime()));
					}
					if (lastUpdateDate != null) {
						igrpctrbase.setLastUpdateDate(lastUpdateDate.getTime());
					}
					igrpctrbaseDao.createOrUpdate(igrpctrbase);
				}
				headerCurrentPage++;
			} while (headerCurrentPage <= totalPage);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	
	/**
	 * 
	 * 查询合同文件信息服务
	 */
	@Override
	public void queryGrpCtrAttrJob(){
		try {
			int totalPage=0;
			BigDecimal headerPageSize=BigDecimal.valueOf(100);//每页大小
			int headerCurrentPage=1;//当前页
			BigDecimal headerTotalRecord=BigDecimal.valueOf(-1);//总页数
			com.cmcc.soa.MsgHeader.MSGHEADER msgHeader = ittokeninfoService.getSOAHeader();
			msgHeader.setPROVINCE_CODE(QueryInputParamCostant.PROVINCE_CODE);
			com.cmcc.soa.OSB_CMS_CMS_HQ_PageInquiryContractDocuSrv.OutputParameters output =null;
			com.cmcc.soa.OSB_CMS_CMS_HQ_PageInquiryContractDocuSrv.InputParameters input=new com.cmcc.soa.OSB_CMS_CMS_HQ_PageInquiryContractDocuSrv.InputParameters();
			input.setPROVINCE_CODE(QueryInputParamCostant.PROVINCE_CODE);
			//增量查询
			input.setLAST_UPDATE_START(WSCostUtilToll.getStartTimeCal());
			input.setLAST_UPDATE_END(WSCostUtilToll.getEndTimeCal());
			int i=0;
			do{
				msgHeader.setPAGE_SIZE(headerPageSize);
				msgHeader.setCURRENT_PAGE(BigDecimal.valueOf(headerCurrentPage));
				msgHeader.setTOTAL_RECORD(headerTotalRecord);
				input.setMSGHEADER(msgHeader);
				output=WSCostUtil.processPageInquiryContractDocuSrv(input);
				//打印执行结果日志
				System.out.println("查询合同文件信息服务+打印ESB 侧执行结果:"+output.getESB_FLAG());
				System.out.println("打印ESB 侧错误代码:"+output.getESB_RETURN_CODE());
				System.out.println("打印系统返回消息:"+output.getESB_RETURN_MESSAGE());
				System.out.println("打印业务服务侧执行结果:"+output.getBIZ_SERVICE_FLAG());
				System.out.println("打印业务服务侧错误代码:"+output.getBIZ_RETURN_CODE());
				System.out.println("打印业务服务返回消息:"+output.getBIZ_RETURN_MESSAGE());
				System.out.println("总页数***************"+output.getTOTAL_PAGE());
				if(output.getTOTAL_PAGE()!=null) {
					totalPage=output.getTOTAL_PAGE().intValue();
				}
				
				com.cmcc.soa.OSB_CMS_CMS_HQ_PageInquiryContractDocuSrv.OUTPUTCOLLECTION_ITEM[] outputCollection=output.getOUTPUTCOLLECTION();
				
				for (com.cmcc.soa.OSB_CMS_CMS_HQ_PageInquiryContractDocuSrv.OUTPUTCOLLECTION_ITEM outputcollection_ITEM : outputCollection) {
					com.cmcc.soa.OSB_CMS_CMS_HQ_PageInquiryContractDocuSrv.CONTRACT_DOC_ITEM[] contractdocitem=outputcollection_ITEM.getCONTRACT_DOC();
			    	for (com.cmcc.soa.OSB_CMS_CMS_HQ_PageInquiryContractDocuSrv.CONTRACT_DOC_ITEM contract_DOC_ITEM : contractdocitem) {
			    		i++;
			    		IGrpCtrAttr igrpctrattr=new IGrpCtrAttr();
			    		igrpctrattr=WSCostUtilToll.switchBusinessEntity2WSEntity(contract_DOC_ITEM,igrpctrattr.getClass() );
			    		Calendar docUploadingDate=contract_DOC_ITEM.getDOC_UPLOADING_DATE();
			    		if(docUploadingDate!=null){
			    			igrpctrattr.setDocUploadingDate(docUploadingDate.getTime());
				    	}
			    		igrpctrattr.setContractName(outputcollection_ITEM.getCONTRACT_NAME());
			    		igrpctrattr.setContractNo(outputcollection_ITEM.getCONTRACT_NO());
			    		igrpctrattr.setContractSerialNo(outputcollection_ITEM.getCONTRACT_SERIAL_NO());
			    		igrpctrattrDao.createOrUpdate(igrpctrattr);
					}
			    }
			headerCurrentPage++;
		}while(headerCurrentPage<=totalPage);
		System.out.println("存储数据的数据条数是："+i);
	} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * 查询应付发票列账信息服务
	 */
	@Override
	public void queryApInvoiceBdgtJob() {
		try {
			//构建增量查询期间，暂为最近三天
			/*Calendar startTimeCal=Calendar.getInstance();
			Calendar endTimeCal=Calendar.getInstance();
			startTimeCal.add(Calendar.DAY_OF_MONTH, -4);
			startTimeCal.set(Calendar.HOUR_OF_DAY, 0);
			startTimeCal.set(Calendar.MINUTE, 0);
			startTimeCal.set(Calendar.SECOND, 0);
			endTimeCal.add(Calendar.DAY_OF_MONTH, -1);
			endTimeCal.set(Calendar.HOUR_OF_DAY, 23);
			endTimeCal.set(Calendar.MINUTE, 59);
			endTimeCal.set(Calendar.SECOND, 59);
			endTimeCal.set(Calendar.MILLISECOND, 999);*/
			Calendar startTimeCal=Calendar.getInstance();
			Calendar endTimeCal=Calendar.getInstance();
			startTimeCal.add(Calendar.DAY_OF_MONTH, -1);
			startTimeCal.set(Calendar.HOUR_OF_DAY, 0);
			startTimeCal.set(Calendar.MINUTE, 0);
			startTimeCal.set(Calendar.SECOND, 0);
			
			int totalPage=0;
			BigDecimal headerPageSize=BigDecimal.valueOf(100);//每页大小
			int headerCurrentPage=1;//当前页
			BigDecimal headerTotalRecord=BigDecimal.valueOf(-1);//总页数
			com.cmcc.soa.MsgHeader.MSGHEADER msgHeader = ittokeninfoService.getSOAHeader();
			msgHeader.setPROVINCE_CODE(QueryInputParamCostant.PROVINCE_CODE);
			msgHeader.setROUTE_CODE(QueryInputParamCostant.ROUTE_CODE);
			com.oracle.xmlns.apps.cux.rest.PageInquiryApInvoiceBdgtSrv.process.OutputParameters output =null;
			com.oracle.xmlns.apps.cux.rest.PageInquiryApInvoiceBdgtSrv.process.InputParameters input=new com.oracle.xmlns.apps.cux.rest.PageInquiryApInvoiceBdgtSrv.process.InputParameters();
			input.setINVOICE_SOURCE("RBS");//报账系统标识
			//增量查询
			input.setLAST_UPDATE_START(startTimeCal);
			input.setLAST_UPDATE_END(endTimeCal);
			com.oracle.xmlns.apps.cux.rest.PageInquiryApInvoiceBdgtSrv.process.PROCESS_Input process_input=new com.oracle.xmlns.apps.cux.rest.PageInquiryApInvoiceBdgtSrv.process.PROCESS_Input();
			do{
				msgHeader.setPAGE_SIZE(headerPageSize);
				msgHeader.setCURRENT_PAGE(BigDecimal.valueOf(headerCurrentPage));
				msgHeader.setTOTAL_RECORD(headerTotalRecord);
				input.setMSGHEADER(msgHeader);
				process_input.setInputParameters(input);
				output=WSCostUtil.processPageInquiryApInvoiceBdgtSrv(process_input);
				//打印执行结果日志
				System.out.println("查询应付发票列账信息服务+打印ESB 侧执行结果:"+output.getESB_FLAG());
				System.out.println("打印ESB 侧错误代码:"+output.getESB_RETURN_CODE());
				System.out.println("打印系统返回消息:"+output.getESB_RETURN_MESSAGE());
				System.out.println("打印业务服务侧执行结果:"+output.getBIZ_SERVICE_FLAG());
				System.out.println("打印业务服务侧错误代码:"+output.getBIZ_RETURN_CODE());
				System.out.println("打印业务服务返回消息:"+output.getBIZ_RETURN_MESSAGE());
				System.out.println("总页数***************"+output.getTOTAL_PAGE());
				if(output.getTOTAL_PAGE()!=null) {
					totalPage=output.getTOTAL_PAGE().intValue();
				}
				com.oracle.xmlns.apps.cux.rest.PageInquiryApInvoiceBdgtSrv.process.OUTPUTCOLLECTION_ITEM[] outputCollection=output.getOUTPUTCOLLECTION();
			    for (com.oracle.xmlns.apps.cux.rest.PageInquiryApInvoiceBdgtSrv.process.OUTPUTCOLLECTION_ITEM outputcollection_ITEM : outputCollection) {
			    	String periodName=outputcollection_ITEM.getPERIOD_NAME();
			    	String sourceDocumentNum=outputcollection_ITEM.getSOURCE_DOCUMENT_NUM();
			    	System.out.println("外部系统单据号为："+sourceDocumentNum);
			    	System.out.println("账期为："+periodName);
			    	if(periodName!=null&&sourceDocumentNum!=null){
			    		periodName=periodName.replaceAll("-", "");
			    		String sql="update T_WF_BNS_AT_MAIN set AT_TERM='"+periodName+"' where FINANCE_NO='"+sourceDocumentNum+"'";
			    		int countNum=this.jdbcTemplate.update(sql);
			    		System.out.println("成功更新"+countNum+"条数据！");
			    	}
			    }
				headerCurrentPage++;
			}while(headerCurrentPage<=totalPage);
			System.out.println("本次查询应付发票列账信息服务执行结束！");
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("查询应付发票列账信息服务出错！");
		}
	}
	
	
	/**
	 * 查询ERP 应付发票信息服务（分页）
	 */
	@Override
	public void queryApInvoiceJob() {
		try {
			SimpleDateFormat simpleDateFormat=new SimpleDateFormat("yyyyMM");
			StringBuffer selectFinanceNoSql=new StringBuffer();
			//第一次执行获取全量的报账单信息
			//selectFinanceNoSql.append("select finance_no from t_wf_bns_at_main where push_state='成功' and finance_no is not null and at_state='COMPLETED'");
			//获取全量的已经推送成功的并且账期为空的全量的报账单信息
			selectFinanceNoSql.append("select finance_no from t_wf_bns_at_main where push_state='成功' and finance_no is not null and at_term is null and at_state='COMPLETED'");
			List<String> selectFinanceNoList=this.jdbcTemplate.queryForList(selectFinanceNoSql.toString(),String.class);
			for (String financeNo : selectFinanceNoList) {
				int totalPage=0;
				BigDecimal headerPageSize=BigDecimal.valueOf(100);//每页大小
				int headerCurrentPage=1;//当前页
				BigDecimal headerTotalRecord=BigDecimal.valueOf(-1);//总页数
				com.cmcc.soa.MsgHeader.MSGHEADER msgHeader = ittokeninfoService.getSOAHeader();
				msgHeader.setPROVINCE_CODE(QueryInputParamCostant.PROVINCE_CODE);
				msgHeader.setROUTE_CODE(QueryInputParamCostant.ROUTE_CODE);
				com.oracle.xmlns.apps.cux.rest.PageInquiryApInvoiceSrv.process.OutputParameters output =null;
				com.oracle.xmlns.apps.cux.rest.PageInquiryApInvoiceSrv.process.InputParameters input=new com.oracle.xmlns.apps.cux.rest.PageInquiryApInvoiceSrv.process.InputParameters();
				input.setSOURCE_DOCUMENT_NUM(financeNo);
				com.oracle.xmlns.apps.cux.rest.PageInquiryApInvoiceSrv.process.PROCESS_Input process_input=new com.oracle.xmlns.apps.cux.rest.PageInquiryApInvoiceSrv.process.PROCESS_Input();
				do{
					msgHeader.setPAGE_SIZE(headerPageSize);
					msgHeader.setCURRENT_PAGE(BigDecimal.valueOf(headerCurrentPage));
					msgHeader.setTOTAL_RECORD(headerTotalRecord);
					input.setMSGHEADER(msgHeader);
					process_input.setInputParameters(input);
					output=WSCostUtil.processPageInquiryApInvoiceSrv(process_input);
					//打印执行结果日志
					System.out.println("查询ERP 应付发票信息服务（分页）+打印ESB 侧执行结果:"+output.getESB_FLAG());
					System.out.println("打印ESB 侧错误代码:"+output.getESB_RETURN_CODE());
					System.out.println("打印系统返回消息:"+output.getESB_RETURN_MESSAGE());
					System.out.println("打印业务服务侧执行结果:"+output.getBIZ_SERVICE_FLAG());
					System.out.println("打印业务服务侧错误代码:"+output.getBIZ_RETURN_CODE());
					System.out.println("打印业务服务返回消息:"+output.getBIZ_RETURN_MESSAGE());
					System.out.println("总页数***************"+output.getTOTAL_PAGE());
					if(output.getTOTAL_PAGE()!=null) {
						totalPage=output.getTOTAL_PAGE().intValue();
					}
					com.oracle.xmlns.apps.cux.rest.PageInquiryApInvoiceSrv.process.OUTPUTCOLLECTION_ITEM[] outputCollection=output.getOUTPUTCOLLECTION();
				    for (com.oracle.xmlns.apps.cux.rest.PageInquiryApInvoiceSrv.process.OUTPUTCOLLECTION_ITEM outputcollection_ITEM : outputCollection) {
				    	Calendar glDate=outputcollection_ITEM.getGL_DATE();
				    	String atTerm=simpleDateFormat.format(glDate.getTime());
				    	String sourceDocumentNum=outputcollection_ITEM.getATTRIBUTE2();
				    	System.out.println("外部系统单据号为："+sourceDocumentNum);
				    	System.out.println("发票账期为："+atTerm);
				    	if(atTerm!=null&&financeNo!=null){
				    		String sql="update T_WF_BNS_AT_MAIN set AT_TERM='"+atTerm+"' where FINANCE_NO='"+sourceDocumentNum+"'";
				    		int countNum=this.jdbcTemplate.update(sql);
				    		System.out.println("成功更新"+countNum+"条数据！");
				    	}
				    }
					headerCurrentPage++;
				}while(headerCurrentPage<=totalPage);
				System.out.println("本次查询ERP 应付发票信息服务（分页）执行结束！");
			}
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("查询ERP 应付发票信息服务（分页）出错！");
		}
	}
	
	
	/**
	 * OSB_ERP_FA_HQ_00001_查询资产账簿信息服务_V0.1
	 */
	@Override
	public void queryGrpErpZcBookJob() {
		try {
			com.cmcc.soa.MsgHeader.MSGHEADER msgHeader = ittokeninfoService.getSOAHeader();
			msgHeader.setPROVINCE_CODE(QueryInputParamCostant.PROVINCE_CODE);
			msgHeader.setROUTE_CODE(QueryInputParamCostant.ROUTE_CODE);
			int pageCount=0;
			BigDecimal headerPageSize=BigDecimal.valueOf(100);//每页大小
			int headerCurrentPage=1;//当前页
			BigDecimal headerTotalRecord=BigDecimal.valueOf(-1);//总页数
			com.oracle.xmlns.apps.cux.rest.InquiryAssetBookSrv.process.InputParameters input=new com.oracle.xmlns.apps.cux.rest.InquiryAssetBookSrv.process.InputParameters();
			com.oracle.xmlns.apps.cux.rest.InquiryAssetBookSrv.process.PROCESS_Input process_input=new com.oracle.xmlns.apps.cux.rest.InquiryAssetBookSrv.process.PROCESS_Input();
			//增量查询
			//input.setLAST_UPDATE_START(WSCostUtilToll.getStartTimeCal());
			//input.setLAST_UPDATE_END(WSCostUtilToll.getEndTimeCal());
			do{
				msgHeader.setPAGE_SIZE(headerPageSize);
				msgHeader.setCURRENT_PAGE(BigDecimal.valueOf(headerCurrentPage));
				msgHeader.setTOTAL_RECORD(headerTotalRecord);
				input.setMSGHEADER(msgHeader);
				process_input.setInputParameters(input);
				com.oracle.xmlns.apps.cux.rest.InquiryAssetBookSrv.process.OutputParameters output=WSCostUtil.processInquiryAssetBookSrv(process_input);
				//打印执行结果日志
				System.out.println("查询资产账簿信息服务+打印ESB 侧执行结果:"+output.getESB_FLAG());
				System.out.println("打印ESB 侧错误代码:"+output.getESB_RETURN_CODE());
				System.out.println("打印系统返回消息:"+output.getESB_RETURN_MESSAGE());
				System.out.println("打印业务服务侧执行结果:"+output.getBIZ_SERVICE_FLAG());
				System.out.println("打印业务服务侧错误代码:"+output.getBIZ_RETURN_CODE());
				System.out.println("打印业务服务返回消息:"+output.getBIZ_RETURN_MESSAGE());
				com.oracle.xmlns.apps.cux.rest.InquiryAssetBookSrv.process.OUTPUTCOLLECTION_ITEM[] outputCollection=output.getOUTPUTCOLLECTION();
				if(output!=null){
					if(outputCollection!=null){
			    	pageCount=outputCollection.length;
				    }else{
				    	pageCount=0;
				    }
				}else{
					pageCount=0;
				}
				for (com.oracle.xmlns.apps.cux.rest.InquiryAssetBookSrv.process.OUTPUTCOLLECTION_ITEM outputcollection_ITEM : outputCollection) {
			    	System.out.println("************查询成功*************");
			    	IGrpErpZcBook igrperpzcbook=new IGrpErpZcBook();
			    	igrperpzcbook=WSCostUtilToll.switchBusinessEntity2WSEntity(outputcollection_ITEM,igrperpzcbook.getClass() );
			    	//更改时间字段
			    	Calendar dateIneffective=outputcollection_ITEM.getDATE_INEFFECTIVE();
			    	Calendar lastDeprnRunDate=outputcollection_ITEM.getLAST_DEPRN_RUN_DATE();
			    	Calendar lastUpateDate=outputcollection_ITEM.getLAST_UPDATE_DATE();
			    	if(dateIneffective!=null){
			    		igrperpzcbook.setDateIneffective(dateIneffective.getTime());
			    	}
			    	if(lastDeprnRunDate!=null){
			    		igrperpzcbook.setLastDeprnRunDate(lastDeprnRunDate.getTime());
			    	}
			    	if(lastUpateDate!=null){
			    		igrperpzcbook.setLastUpateDate(lastUpateDate.getTime());
			    	}
			    	//将账簿编码更新至id
			    	igrperpzcbook.setId(outputcollection_ITEM.getBOOK_TYPE_CODE());
			    	try {
			    		igrperpzcbookDao.createOrUpdate(igrperpzcbook);
					} catch (Exception e) {
						e.printStackTrace();
						System.out.println("保存资产账簿信息出错！");
					}
			    }
				headerCurrentPage++;
			}while(pageCount%100==1);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * 查询合同预算信息服务
	 * 增量查询
	 */
	@Override
	public void queryGrpCtrBudgetJob() {
		try {
			com.cmcc.soa.OSB_CMS_CMS_HQ_PageInquiryContractBudgetSrv.InputParameters input = new com.cmcc.soa.OSB_CMS_CMS_HQ_PageInquiryContractBudgetSrv.InputParameters();
			com.cmcc.soa.MsgHeader.MSGHEADER msgHeader = ittokeninfoService.getSOAHeader();
			msgHeader.setPROVINCE_CODE(QueryInputParamCostant.PROVINCE_CODE);
			input.setPROVINCE_CODE(QueryInputParamCostant.PROVINCE_CODE);
			StringBuffer selectOU = new StringBuffer();
			selectOU.append("select company_code from i_grp_erp_ou");
			List<String> ous = this.jdbcTemplate.queryForList(selectOU.toString(), String.class);
			for (String ou : ous) {
				input.setLAST_UPDATE_START(WSCostUtilToll.getStartTimeCal());
				input.setLAST_UPDATE_END(WSCostUtilToll.getEndTimeCal());
				int totalPage = 0;
				BigDecimal headerPageSize = BigDecimal.valueOf(200);// 每页大小
				int headerCurrentPage = 1;// 当前页
				BigDecimal headerTotalRecord = BigDecimal.valueOf(-1);// 总页数
				do {
					msgHeader.setPAGE_SIZE(headerPageSize);
					msgHeader.setCURRENT_PAGE(BigDecimal.valueOf(headerCurrentPage));
					msgHeader.setTOTAL_RECORD(headerTotalRecord);
					input.setMSGHEADER(msgHeader);
					com.cmcc.soa.OSB_CMS_CMS_HQ_PageInquiryContractBudgetSrv.OutputParameters output = WSCostUtil
							.processPageInquiryContractBudgetSrv(input);
					// 打印执行结果日志
					System.out.println("查询合同预算信息服务打印ESB侧执行结果:" + output.getESB_FLAG());
					System.out.println("打印ESB侧错误代码:" + output.getESB_RETURN_CODE());
					System.out.println("打印系统返回消息:" + output.getESB_RETURN_MESSAGE());
					System.out.println("打印业务服务侧执行结果:" + output.getBIZ_SERVICE_FLAG());
					System.out.println("打印业务服务侧错误代码:" + output.getBIZ_RETURN_CODE());
					System.out.println("打印业务服务返回消息:" + output.getBIZ_RETURN_MESSAGE());
					System.out.println("======================总页数:" + output.getTOTAL_PAGE());
					if (output.getTOTAL_PAGE() != null) {
						totalPage = output.getTOTAL_PAGE().intValue();
					}
					if ("TRUE".equalsIgnoreCase(output.getBIZ_SERVICE_FLAG())) {
						com.cmcc.soa.OSB_CMS_CMS_HQ_PageInquiryContractBudgetSrv.OUTPUTCOLLECTION_ITEM[] outputCollection = output
								.getOUTPUTCOLLECTION();
						for (com.cmcc.soa.OSB_CMS_CMS_HQ_PageInquiryContractBudgetSrv.OUTPUTCOLLECTION_ITEM outputcollection_ITEM : outputCollection) {
							com.cmcc.soa.OSB_CMS_CMS_HQ_PageInquiryContractBudgetSrv.BUDGET_LINE_ITEM[] budgetLineItem = outputcollection_ITEM
									.getBUDGET_LINE();
							IGrpCtrBudget igrpctrbudget = new IGrpCtrBudget();
							igrpctrbudget = WSCostUtilToll.switchBusinessEntity2WSEntity(outputcollection_ITEM,
									igrpctrbudget.getClass());
							for (com.cmcc.soa.OSB_CMS_CMS_HQ_PageInquiryContractBudgetSrv.BUDGET_LINE_ITEM budget_LINE_ITEM : budgetLineItem) {
								igrpctrbudget.setLineNum(budget_LINE_ITEM.getLINE_NUM());
								if (budget_LINE_ITEM.getTAX_AMOUNT() != null) {
									igrpctrbudget.setTaxAmount(budget_LINE_ITEM.getTAX_AMOUNT().doubleValue());
								}
								if (budget_LINE_ITEM.getAMOUNT() != null) {
									igrpctrbudget.setAmount(budget_LINE_ITEM.getAMOUNT().doubleValue());
								}
								igrpctrbudget.setTaxRate(budget_LINE_ITEM.getTAX_RATE());
								igrpctrbudget.setPriKey(budget_LINE_ITEM.getPRI_KEY());
								igrpctrbudget.setBudgetYear(budget_LINE_ITEM.getBUDGET_YEAR());
								igrpctrbudget.setBudgetProjectNum(budget_LINE_ITEM.getBUDGET_PROJECT_NUM());
								igrpctrbudget.setBudgetProjectName(budget_LINE_ITEM.getBUDGET_PROJECT_NAME());
								igrpctrbudget.setBudgetDeptCode(budget_LINE_ITEM.getBUDGET_DEPT_CODE());
								igrpctrbudget.setBudgetDeptName(budget_LINE_ITEM.getBUDGET_DEPT_NAME());
								igrpctrbudget.setActivityCode(budget_LINE_ITEM.getACTIVITY_CODE());
								igrpctrbudget.setActivityName(budget_LINE_ITEM.getACTIVITY_NAME());
								igrpctrbudget.setCompanyCode(budget_LINE_ITEM.getCOMPANY_CODE());
								igrpctrbudgetDao.createOrUpdate(igrpctrbudget);
							}
						}
					}
					headerCurrentPage++;
				} while (headerCurrentPage <= totalPage);
			}
			System.out.println("本次查询合同预算信息服务结束");
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("本次查询合同预算信息服务出错");
		}
	}
	
	
	/**
	 *TODO 获取服务token值
	 * @param map 查询条件
	 * @return
	 * @throws Exception 
	 */
	@Override
	public void getTokenInfo(){
		com.cmcc.soa.OSB_BP_SOA_HQ_InquiryServiceTokenSrv.InputParameters input = new com.cmcc.soa.OSB_BP_SOA_HQ_InquiryServiceTokenSrv.InputParameters();
		try {
			SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
			SimpleDateFormat sdf1 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			String tokenSql="SELECT TOKEN_ID ,INVALID_PERIOD FROM T_TOKEN_INFO WHERE IS_USE='1'";
			Map<String, Object> tokenInfo=this.jdbcTemplate.queryForMap(tokenSql);
			String invalidPeriodStr =tokenInfo.get("INVALID_PERIOD").toString();
			Date invalidPeriodDate=sdf1.parse(invalidPeriodStr);
			System.out.println("运算之前旧token的失效时间为："+sdf1.format(invalidPeriodDate));
			Calendar invalidPeriodCal=Calendar.getInstance();
			invalidPeriodCal.setTime(invalidPeriodDate);
			invalidPeriodCal.add(Calendar.DAY_OF_MONTH, -15);
			System.out.println("运算后旧token失效时间为："+sdf1.format(invalidPeriodCal.getTime()));
			System.out.println("是否失效标识为："+invalidPeriodCal.before(Calendar.getInstance()));
			if (invalidPeriodCal.before(Calendar.getInstance())) {
				String oldToken=tokenInfo.get("TOKEN_ID").toString();
				System.out.println("旧token为："+oldToken);
				com.cmcc.soa.MsgHeader.MSGHEADER msgheader= new MSGHEADER();
				msgheader.setSOURCESYSTEMID(QueryInputParamCostant.SOURCESYSTEMID);
				msgheader.setSOURCESYSTEMNAME(QueryInputParamCostant.SOURCESYSTEMNAME);
				msgheader.setPROVINCE_CODE(QueryInputParamCostant.PROVINCE_CODE);
				msgheader.setTOKEN(DigestUtils.md5DigestAsHex((oldToken+sdf.format(new java.util.Date())).getBytes()));
				input.setMSGHEADER(msgheader);
				input.setSYSTEM_CODE(QueryInputParamCostant.SYSTEM_CODE);//业务系统代码，必填  
				input.setPROVINCE_CODE(QueryInputParamCostant.PROVINCE_CODE);//业务系统省份代码，必填
				input.setCURRENT_TOKEN(oldToken);//当前 TOKEN，必填
				com.cmcc.soa.OSB_BP_SOA_HQ_InquiryServiceTokenSrv.OutputParameters output =
						WSCostUtil.processInquiryServiceTokenSrv(input);
				System.out.println("查询token信息服务打印ESB侧执行结果:" + output.getESB_FLAG());
				System.out.println("打印ESB侧错误代码:" + output.getESB_RETURN_CODE());
				System.out.println("打印系统返回消息:" + output.getESB_RETURN_MESSAGE());
				System.out.println("打印业务服务侧执行结果:" + output.getBIZ_SERVICE_FLAG());
				System.out.println("打印业务服务侧错误代码:" + output.getBIZ_RETURN_CODE());
				System.out.println("打印业务服务返回消息:" + output.getBIZ_RETURN_MESSAGE());
				com.cmcc.soa.OSB_BP_SOA_HQ_InquiryServiceTokenSrv.OUTPUTCOLLECTION_ITEM [] outs = output.getOUTPUTCOLLECTION();
				int i = 1;
				for(com.cmcc.soa.OSB_BP_SOA_HQ_InquiryServiceTokenSrv.OUTPUTCOLLECTION_ITEM out : outs){
					System.out.println("这是第***********"+i+"**********个结果集！！！！！！！！！");
					System.out.println("查询获得的新token**************"+out.getNEW_TOKEN());
					System.out.println("查询获得的新token的失效日期***********"+sdf1.format(out.getINVALID_PERIOD().getTime()));
					Calendar cal = Calendar.getInstance();
					//有效期90天
					cal.add(Calendar.DATE, 90);
					String tokenId=out.getNEW_TOKEN()!=null?out.getNEW_TOKEN():"";
					//失效期
					Date invalidPeriodNewCal=out.getINVALID_PERIOD()!=null?out.getINVALID_PERIOD():cal.getTime();
					System.out.println("计算获得的新token的失效日期***********"+sdf1.format(invalidPeriodNewCal));
					//更新旧token的有效位
					this.jdbcTemplate.update("update t_token_info set is_use='0' where token_id=?",oldToken);
					//新token保存
					StringBuffer insertSql=new StringBuffer();
					insertSql.append("insert into t_token_info (token_id,invalid_period,create_time,is_use) values (?,?,sysdate,'1')");
					this.jdbcTemplate.update(insertSql.toString(),tokenId,invalidPeriodNewCal);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("调用获取新token接口出错！");
		}
	}
	/**
	 * 查询固定资产折旧信息服务（分页）
	 */
	@Override
	public void queryPageInquiryAssetDeprecationSrv() {
		try {
			List<IGrpErpZcBook> list = igrperpzcbookService.findAll();
			com.oracle.xmlns.apps.cux.rest.PageInquiryAssetDeprecationSrv.process.InputParameters input = new com.oracle.xmlns.apps.cux.rest.PageInquiryAssetDeprecationSrv.process.InputParameters();
			com.oracle.xmlns.apps.cux.rest.PageInquiryAssetDeprecationSrv.process.PROCESS_Input process_input = new com.oracle.xmlns.apps.cux.rest.PageInquiryAssetDeprecationSrv.process.PROCESS_Input();
			// 填充头信息
			com.cmcc.soa.MsgHeader.MSGHEADER msgHeader = ittokeninfoService.getSOAHeader();
			msgHeader.setPROVINCE_CODE(QueryInputParamCostant.PROVINCE_CODE);
			msgHeader.setROUTE_CODE(QueryInputParamCostant.ROUTE_CODE);
			for (IGrpErpZcBook iGrpErpZcBook : list) {
				String bookTypeCode = iGrpErpZcBook.getBookTypeCode();
				System.out.println("本次查询的账簿类型代码为：" + bookTypeCode);
				input.setBOOK_TYPE_CODE(bookTypeCode);
				BigDecimal headerPageSize = BigDecimal.valueOf(200);// 每页大小
				int currentPage = 1;// 当前页
				int totalPage = -1;// 总页数
				String isSuccess = "TRUE";
				do {
					msgHeader.setPAGE_SIZE(headerPageSize);
					msgHeader.setCURRENT_PAGE(BigDecimal.valueOf(currentPage));
					msgHeader.setTOTAL_RECORD(BigDecimal.valueOf(totalPage));
					input.setMSGHEADER(msgHeader);
					process_input.setInputParameters(input);
					com.oracle.xmlns.apps.cux.rest.PageInquiryAssetDeprecationSrv.process.OutputParameters output = WSCostUtil
							.processPageInquiryAssetDeprecationSrv(process_input);
					isSuccess = output.getBIZ_SERVICE_FLAG();
					System.out.println("循环标识为" + isSuccess);
					if (output.getTOTAL_PAGE() != null) {
						totalPage = output.getTOTAL_PAGE().intValue();
					}
					if ("TRUE".equals(output.getBIZ_SERVICE_FLAG()) && "TRUE".equals(output.getESB_FLAG())) {
						System.out.println("打印ESB侧执行结果:" + output.getESB_FLAG());
						System.out.println("打印BIZ侧执行结果:" + output.getBIZ_SERVICE_FLAG());
						System.out.println("********************总页数***************" + output.getTOTAL_PAGE());
						System.out.println("********************当前页***************" + currentPage);
						com.oracle.xmlns.apps.cux.rest.PageInquiryAssetDeprecationSrv.process.OUTPUTCOLLECTION_ITEM[] outputCollection = output
								.getOUTPUTCOLLECTION();
						for (com.oracle.xmlns.apps.cux.rest.PageInquiryAssetDeprecationSrv.process.OUTPUTCOLLECTION_ITEM col : outputCollection) {
							System.out.println("********************************开始循环打印固定资产折旧信息**********************************");
							Class cls2 = col.getClass();
							Field[] fields2 = cls2.getDeclaredFields();  
							for(int i=0; i<fields2.length; i++){  
								Field f = fields2[i];  
								f.setAccessible(true);  
								System.out.println("属性名:" + f.getName() + " 属性值:" + f.get(col));  
							}
							System.out.println("********************************循环打印固定资产折旧信息结束**********************************");
						}
					} else {
						System.out.println("打印ESB侧执行结果:" + output.getESB_FLAG());
						System.out.println("打印ESB侧错误代码:" + output.getESB_RETURN_CODE());
						System.out.println("打印系统返回消息" + output.getESB_RETURN_MESSAGE());
						System.out.println("打印BIZ侧执行结果:" + output.getBIZ_SERVICE_FLAG());
						System.out.println("打印BIZ侧错误代码:" + output.getBIZ_RETURN_CODE());
						System.out.println("打印BIZ返回消息:" + output.getBIZ_RETURN_MESSAGE());
						break;
					}
					currentPage++;
				} while (currentPage <= totalPage);
				System.out.println("本次按账簿查询执行完毕！！！");
			}
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("本次按账簿查询执行出错！！！");
		}
	}
	
	/**
	 * 查询计提单报账系统的T2行ID
	 */
	@Override
	public void queryParentExpenseLineId() {
		try {
			StringBuffer selectCompleteWdSql=new StringBuffer();
			selectCompleteWdSql.append("select t.wf_no from t_wf_bns_wd_main t left join t_wf_bns_wd_line t1 on t.wf_no=t1.wf_no where t.wd_state='COMPLETED' and t1.parent_expense_line_id is null");
			List<String> atList=this.jdbcTemplate.queryForList(selectCompleteWdSql.toString(),String.class);
			for (String wfNo : atList) {
				com.cmcc.soa.OSB_RBS_CMF_HQ_PageInquiryClaimDocRstSrv.OutputParameters output=ittokeninfoService.getBaoZhangDanImportResult(wfNo);
				if ("TRUE".equals(output.getBIZ_SERVICE_FLAG())&&"TRUE".equals(output.getESB_FLAG())) {
					com.cmcc.soa.OSB_RBS_CMF_HQ_PageInquiryClaimDocRstSrv.OUTPUTCOLLECTION_ITEM [] collection = output.getOUTPUTCOLLECTION();
					for (com.cmcc.soa.OSB_RBS_CMF_HQ_PageInquiryClaimDocRstSrv.OUTPUTCOLLECTION_ITEM col : collection) {
						com.cmcc.soa.OSB_RBS_CMF_HQ_PageInquiryClaimDocRstSrv.CLAIM_LINE_INFO_ITEM [] claimLinesItem  = col.getCLAIM_LINE_INFO();
						for (com.cmcc.soa.OSB_RBS_CMF_HQ_PageInquiryClaimDocRstSrv.CLAIM_LINE_INFO_ITEM claim : claimLinesItem) {
							StringBuffer updateT2Id=new StringBuffer();
							updateT2Id.append("update t_wf_bns_wd_line set parent_expense_line_id = ? where wf_no = ? and pri_key=?");
							int numCount=this.jdbcTemplate.update(updateT2Id.toString(),claim.getPARENT_EXPENSE_LINE_ID(),wfNo,claim.getPRI_KEY());
							System.out.println("成功更新"+numCount+"条计提行信息！");
						}
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("查询报账单导入结果信息服务出错");
		}
	}
}
