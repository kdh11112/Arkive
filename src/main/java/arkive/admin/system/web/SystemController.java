package arkive.admin.system.web;

import org.egovframe.rte.fdl.property.EgovPropertyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
@RequestMapping("/system")
public class SystemController {

	protected final Logger logger = LoggerFactory.getLogger(getClass());
	
	@Resource(name = "propertiesService")
	protected EgovPropertyService propertiesService;
	
	public static int pageUnit = 10;
	public static int pageSize = 5;
}
