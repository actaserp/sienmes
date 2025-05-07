package mes.app.account;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

import mes.domain.entity.User;
import mes.domain.model.AjaxResult;
import mes.domain.repository.UserRepository;
import mes.domain.security.CustomAuthenticationToken;
import mes.domain.security.Pbkdf2Sha256;
import mes.domain.services.AccountService;
import mes.domain.services.SqlRunner;


@RestController
public class AccountController {
	
	@Autowired
	AccountService accountService;
		
    @Autowired
    UserRepository userRepository;
	
	@Autowired
	SqlRunner sqlRunner;

	private final ConcurrentHashMap<String, String> tokenStore = new ConcurrentHashMap<>();
	private final ConcurrentHashMap<String, Long> tokenExpiry = new ConcurrentHashMap<>();
	private Boolean flag;
	private Boolean flag_pw;
	
	@Resource(name="authenticationManager")
    private AuthenticationManager authManager;
	
	@GetMapping("/login")
    public ModelAndView loginPage(
    		HttpServletRequest request,
    		HttpServletResponse response,
    		HttpSession session, Authentication auth) {

		//User-Agent를 기반으로 모바일 여부 감지
		String userAgent = request.getHeader("User-Agent").toLowerCase();
		boolean isMobile = userAgent.contains("mobile") || userAgent.contains("android") || userAgent.contains("iphone");

		// 세션을 이용해 모바일에서 한 번만 리디렉션되도록 설정
		Boolean isMobileRedirected = (Boolean) session.getAttribute("isMobileRedirected");

		if (isMobile && (isMobileRedirected == null || !isMobileRedirected)) {
			session.setAttribute("isMobileRedirected", true);  // ✅ 모바일에서 리디렉션 상태 저장
			return new ModelAndView("redirect:/MobileFirstPage");
		}

		// 모바일이면 "mlogin" 뷰 반환, 웹이면 "login" 뷰 반환
		ModelAndView mv = new ModelAndView(isMobile ? "mlogin" : "login");
		
		Map<String, Object> userInfo = new HashMap<String, Object>(); 
		Map<String, Object> gui = new HashMap<String, Object>();
		
		mv.addObject("userinfo", userInfo);
		mv.addObject("gui", gui);
		if(auth!=null) {
			SecurityContextLogoutHandler handler =  new SecurityContextLogoutHandler();
			handler.logout(request, response, auth);
		}
		
		return mv;
	}

	@GetMapping("/MobileFirstPage")
	public ModelAndView mobileFirstPage(HttpSession session) {
		session.removeAttribute("isMobileRedirected");  // ✅ 모바일 첫 페이지에서 세션 값 초기화
		return new ModelAndView("/mobile/MobileFirstPage");
	}
	
	@GetMapping("/logout")
	public void logout(
			HttpServletRequest request
			, HttpServletResponse response) throws IOException {
		
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();		
		SecurityContextLogoutHandler handler =  new SecurityContextLogoutHandler();
		
		this.accountService.saveLoginLog("logout", auth);
		
		handler.logout(request, response, auth);
	    response.sendRedirect("/login");
	}

    @PostMapping("/login")
    public AjaxResult postLogin(
    		@RequestParam("username") final String username, 
    		@RequestParam("password") final String password,
    		final HttpServletRequest request) {
    	// 여기로 들어오지 않음.
    	
    	AjaxResult result = new AjaxResult();
    	
    	HashMap<String, Object> data = new HashMap<String, Object>();
    	result.data = data;
    	
        UsernamePasswordAuthenticationToken authReq = new UsernamePasswordAuthenticationToken(username, password);
		CustomAuthenticationToken auth = null;

		try{
			auth = (CustomAuthenticationToken)authManager.authenticate(authReq);
		}catch (AuthenticationException e){
			//e.printStackTrace();
			data.put("code", "NOUSER");
			return result;
		}


		if(auth!=null) {
			User user = (User)auth.getPrincipal();
			if (!user.getActive()) {  // user.getActive()가 false인 경우
				data.put("code", "noactive");
			} else {
				data.put("code", "OK");

				try {
					this.accountService.saveLoginLog("login", auth);
				} catch (UnknownHostException e) {
					// Handle the exception (e.g., log it)
					e.printStackTrace();
				}
			}
		} else {
			result.success=false;
			data.put("code", "NOID");
		}

		SecurityContext sc = SecurityContextHolder.getContext();
		sc.setAuthentication(auth);

		HttpSession session = request.getSession(true);
		session.setAttribute("SPRING_SECURITY_CONTEXT", sc);

		return result;
	}
    
    @GetMapping("/account/myinfo")
    public AjaxResult getUserInfo(Authentication auth){
    	User user = (User)auth.getPrincipal();
    	AjaxResult result = new AjaxResult();
    	
    	Map<String, Object> dicData = new HashMap<String, Object>();
    	dicData.put("login_id", user.getUsername());
    	dicData.put("name", user.getUserProfile().getName());    	
    	result.data = dicData;    	
    	return result;    	
    }
    
    @PostMapping("/account/myinfo/password_change")
    public AjaxResult userPasswordChange(
    		@RequestParam("name") final String name,
    		@RequestParam("loginPwd") final String loginPwd,
    		@RequestParam("loginPwd2") final String loginPwd2,    		
    		Authentication auth
    		) {
    	
    	User user = (User)auth.getPrincipal();
        AjaxResult result = new AjaxResult();
        
        if (StringUtils.hasText(loginPwd)==false | StringUtils.hasText(loginPwd2)==false) {
        	result.success=false;
        	result.message="The verification password is incorrect.";
        	return result;
        }
        
        if(loginPwd.equals(loginPwd2)==false) {        	
        	result.success=false;
        	result.message="The verification password is incorrect.";
        	return result;
        }
        
        user.setPassword(Pbkdf2Sha256.encode(loginPwd2));        
        //user.getUserProfile().setName(name);
        this.userRepository.save(user);
        
        String sql = """
        	update user_profile set 
        	"Name"=:name, _modified = now(), _modifier_id=:id 
        	where id=:id 
        """;
        
        MapSqlParameterSource dicParam = new MapSqlParameterSource();
        dicParam.addValue("name", name);
        dicParam.addValue("id", user.getId());
        this.sqlRunner.execute(sql, dicParam);
        
        
        return result;
    }

	/***
	 *  아이디 중복 확인
	 * **/
	@PostMapping("/useridchk")
	public AjaxResult IdChk(@RequestParam("userid") final String userid){

		AjaxResult result = new AjaxResult();


		Optional<User> user = userRepository.findByUsername(userid);


		if(!user.isPresent()){

			result.success = true;
			result.message = "사용할 수 있는 계정입니다.";
			return result;

		}else {
			result.success = false;
			result.message = "중복된 계정이 존재합니다.";
			return result;
		}


	}

	@PostMapping("/authentication")
	public AjaxResult Authentication(@RequestParam(value = "AuthenticationCode") String AuthenticationCode,
									 @RequestParam(value = "email", required = false) String email,
									 @RequestParam String type
	){

		AjaxResult result = verifyAuthenticationCode(AuthenticationCode, email);

		if(type.equals("new")){
			if(result.success){
				flag = true;
				result.message = "인증되었습니다.";

			}

		}else{
			if(result.success){
				flag_pw = true;
				result.message = "인증되었습니다.";
			}
		}

		return result;
	}

	private AjaxResult verifyAuthenticationCode(String code, String mail){

		AjaxResult result = new AjaxResult();

		String storedToken = tokenStore.get(mail);
		if(storedToken != null && storedToken.equals(code)){
			long expiryTime = tokenExpiry.getOrDefault(mail, 0L);
			if(System.currentTimeMillis() > expiryTime){
				result.success = false;
				result.message = "인증 코드가 만료되었습니다.";
				tokenStore.remove(mail);
				tokenExpiry.remove(mail);
			} else {
				result.success = true;
				result.message = "비밀번호가 변경되었습니다.";
			}
		}else{
			result.success = false;
			result.message = "인증 코드가 유효하지 않습니다.";
		}
		return result;
	}
}