package com.fangyicha.controller.auth;

import com.fangyicha.common.Constants;
import com.fangyicha.common.Result;
import com.fangyicha.dto.LoginRequest;
import com.fangyicha.dto.LoginResponse;
import com.fangyicha.dto.RegisterRequest;
import com.fangyicha.entity.Customer;
import com.fangyicha.entity.Developer;
import com.fangyicha.security.JwtUtil;
import com.fangyicha.service.CustomerService;
import com.fangyicha.service.DeveloperService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 认证控制器
 * 处理登录、注册、获取当前用户信息
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
@Tag(name = "认证管理", description = "登录、注册、用户信息查询")
public class AuthController {

    private final DeveloperService developerService;
    private final CustomerService customerService;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;

    public AuthController(DeveloperService developerService,
                          CustomerService customerService,
                          JwtUtil jwtUtil,
                          PasswordEncoder passwordEncoder) {
        this.developerService = developerService;
        this.customerService = customerService;
        this.jwtUtil = jwtUtil;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * 用户登录
     * 支持开发商和客户两种角色登录
     */
    @PostMapping("/login")
    @Operation(summary = "用户登录", description = "根据角色(developer/customer)登录，返回JWT令牌")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        String role = request.getRole();
        if (role == null || role.trim().isEmpty()) {
            role = "developer";
        }

        LoginResponse loginResponse;

        if ("developer".equals(role)) {
            // 开发商登录
            Developer developer = developerService.getByUsername(request.getUsername());
            if (developer == null) {
                return Result.unauthorized("用户名或密码错误");
            }
            if (developer.getStatus() != null && developer.getStatus() == 0) {
                return Result.forbidden("账号已被禁用");
            }
            if (!passwordEncoder.matches(request.getPassword(), developer.getPassword())) {
                return Result.unauthorized("用户名或密码错误");
            }

            String token = jwtUtil.generateToken(developer.getId(), developer.getUsername(), Constants.ROLE_DEVELOPER);
            loginResponse = new LoginResponse(token, developer.getId(), developer.getUsername(),
                    Constants.ROLE_DEVELOPER, developer.getCompanyName());

        } else if ("customer".equals(role)) {
            // 客户登录
            Customer customer = customerService.getByUsername(request.getUsername());
            if (customer == null) {
                return Result.unauthorized("用户名或密码错误");
            }
            if (customer.getStatus() != null && customer.getStatus() == 0) {
                return Result.forbidden("账号已被禁用");
            }
            if (!passwordEncoder.matches(request.getPassword(), customer.getPassword())) {
                return Result.unauthorized("用户名或密码错误");
            }

            String token = jwtUtil.generateToken(customer.getId(), customer.getUsername(), Constants.ROLE_CUSTOMER);
            loginResponse = new LoginResponse(token, customer.getId(), customer.getUsername(),
                    Constants.ROLE_CUSTOMER, customer.getRealName());

        } else {
            return Result.badRequest("无效的角色类型");
        }

        log.info("用户登录成功: username={}, role={}", request.getUsername(), role);
        return Result.success("登录成功", loginResponse);
    }

    /**
     * 客户注册
     */
    @PostMapping("/register")
    @Operation(summary = "客户注册", description = "新客户注册账号")
    public Result<Map<String, Object>> register(@Valid @RequestBody RegisterRequest request) {
        // 校验协议
        if (request.getAgreement() == null || !request.getAgreement()) {
            return Result.badRequest("请同意用户协议");
        }
        // 校验密码一致性
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            return Result.badRequest("两次输入的密码不一致");
        }
        // 校验用户名唯一性
        if (customerService.checkUsernameExists(request.getUsername())) {
            return Result.badRequest("用户名已存在");
        }

        Customer customer = customerService.register(request);
        Map<String, Object> data = new HashMap<>();
        data.put("id", customer.getId());
        data.put("username", customer.getUsername());

        log.info("客户注册成功: username={}", customer.getUsername());
        return Result.success("注册成功，请登录", data);
    }

    /**
     * 检查用户名是否已存在
     */
    @GetMapping("/check-username")
    @Operation(summary = "检查用户名", description = "检查用户名是否已被注册")
    public Result<Map<String, Boolean>> checkUsername(@RequestParam String username) {
        boolean exists = customerService.checkUsernameExists(username);
        Map<String, Boolean> data = new HashMap<>();
        data.put("exists", exists);
        return Result.success(data);
    }

    /**
     * 获取当前登录用户信息
     */
    @GetMapping("/me")
    @Operation(summary = "当前用户信息", description = "获取当前登录用户的详细信息")
    public Result<Map<String, Object>> getCurrentUser(Authentication authentication) {
        if (authentication == null) {
            return Result.unauthorized("未登录");
        }

        Map<String, Object> userInfo = new HashMap<>();
        Long userId = (Long) authentication.getPrincipal();
        String role = authentication.getAuthorities().stream()
                .findFirst()
                .map(a -> a.getAuthority())
                .orElse("");

        userInfo.put("userId", userId);
        userInfo.put("role", role);

        if (Constants.ROLE_DEVELOPER.equals(role)) {
            Developer developer = developerService.getById(userId);
            if (developer != null) {
                userInfo.put("username", developer.getUsername());
                userInfo.put("displayName", developer.getCompanyName());
                userInfo.put("companyName", developer.getCompanyName());
                userInfo.put("contactPerson", developer.getContactPerson());
                userInfo.put("phone", developer.getPhone());
                userInfo.put("email", developer.getEmail());
            }
        } else if (Constants.ROLE_CUSTOMER.equals(role)) {
            Customer customer = customerService.getById(userId);
            if (customer != null) {
                userInfo.put("username", customer.getUsername());
                userInfo.put("displayName", customer.getRealName());
                userInfo.put("realName", customer.getRealName());
                userInfo.put("phone", customer.getPhone());
                userInfo.put("email", customer.getEmail());
            }
        }

        return Result.success(userInfo);
    }
}
