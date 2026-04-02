package arkive.com.controller;

import org.egovframe.rte.fdl.property.EgovPropertyService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;


@Controller
@Slf4j
public class ArkiveController {

	/** EgovPropertyService */
	@Resource(name = "propertiesService")
	private EgovPropertyService propertiesService;
	
	@RequestMapping("/")
	public String index(ModelMap model) throws Exception {
		return "bootstrap/index";
	}

	@RequestMapping("/buttons")
	public String buttons(ModelMap model) throws Exception {
		return "bootstrap/buttons";
	}
	
	@RequestMapping("/cards")
	public String cards(ModelMap model) throws Exception {
		return "bootstrap/cards";
	}
	
	@RequestMapping("/color")
	public String color(ModelMap model) throws Exception {
		return "bootstrap/utilities-color";
	}
	@RequestMapping("/border")
	public String border(ModelMap model) throws Exception {
		return "bootstrap/utilities-border";
	}
	@RequestMapping("/animation")
	public String animation(ModelMap model) throws Exception {
		return "bootstrap/utilities-animation";
	}
	@RequestMapping("/other")
	public String other(ModelMap model) throws Exception {
		return "bootstrap/utilities-other";
	}
	@RequestMapping("/login")
	public String login(ModelMap model) throws Exception {
		return "bootstrap/login";
	}
	@RequestMapping("/register")
	public String register(ModelMap model) throws Exception {
		return "bootstrap/register";
	}
	@RequestMapping("/password")
	public String password(ModelMap model) throws Exception {
		return "bootstrap/forgot-password";
	}
	@RequestMapping("/not")
	public String not(ModelMap model) throws Exception {
		return "bootstrap/404";
	}
	@RequestMapping("/blank")
	public String blank(ModelMap model) throws Exception {
		return "bootstrap/blank";
	}
	@RequestMapping("/charts")
	public String charts(ModelMap model) throws Exception {
		return "bootstrap/charts";
	}
	@RequestMapping("/tables")
	public String tables(ModelMap model) throws Exception {
		return "bootstrap/tables";
	}
}
