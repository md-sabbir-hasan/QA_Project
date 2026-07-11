package com.nexaerp.config;

import com.nexaerp.permission.Permission;
import com.nexaerp.permission.PermissionRepository;
import com.nexaerp.role.Role;
import com.nexaerp.role.RoleRepository;
import com.nexaerp.user.User;
import com.nexaerp.user.UserRepository;
import com.nexaerp.user.UserStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;


@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final PermissionRepository permissionRepository;
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.default-admin.email}")
    private String adminEmail;

    @Value("${app.default-admin.password}")
    private String adminPassword;

    @Override
    public void run(String... args) throws Exception {
        seedPermissions();
        seedRoles();
        seedAdminUser();
        // Default account mappings (DEFAULT_RECEIVABLE_ACCOUNT etc.) are no
        // longer auto-seeded here - they depend on the company's own Chart
        // of Accounts, which doesn't exist yet on a fresh install. They must
        // be configured once from Settings -> Default Accounts; until then,
        // any feature that needs one throws a clear "not configured" error.
    }


    // -----Assign Permission-----


    private void seedPermissions() {

        List<Object[]> permissions = List.of(
                // code, name, module
                new Object[]{"VIEW_ACCOUNTS", "View Accounts", "ACCOUNT"},
                new Object[]{"CREATE_ACCOUNT", "Create Account", "ACCOUNT"},
                new Object[]{"EDIT_ACCOUNT", "Edit Account", "ACCOUNT"},
                new Object[]{"DEACTIVATE_ACCOUNT", "Deactivate Account", "ACCOUNT"},

                new Object[]{"VIEW_JOURNAL", "View Journal", "JOURNAL"},
                new Object[]{"CREATE_JOURNAL", "Create Journal", "JOURNAL"},
                new Object[]{"POST_JOURNAL", "Post Journal", "JOURNAL"},
                new Object[]{"REVERSE_JOURNAL", "Reverse Journal", "JOURNAL"},
                new Object[]{"DELETE_JOURNAL", "Delete Journal", "JOURNAL"},

                new Object[]{"VIEW_PARTY", "View Party", "PARTY"},
                new Object[]{"CREATE_PARTY", "Create Party", "PARTY"},
                new Object[]{"EDIT_PARTY", "Edit Party", "PARTY"},
                new Object[]{"DEACTIVATE_PARTY", "Deactivate Party", "PARTY"},

                new Object[]{"VIEW_INVOICE", "View Invoice", "INVOICE"},
                new Object[]{"CREATE_INVOICE", "Create Invoice", "INVOICE"},
                new Object[]{"EDIT_INVOICE", "Edit Invoice", "INVOICE"},
                new Object[]{"POST_INVOICE", "Post Invoice", "INVOICE"},
                new Object[]{"CANCEL_INVOICE", "Cancel Invoice", "INVOICE"},

                new Object[]{"VIEW_VENDOR_BILL", "View Vendor Bill", "VENDOR_BILL"},
                new Object[]{"CREATE_VENDOR_BILL", "Create Vendor Bill", "VENDOR_BILL"},
                new Object[]{"EDIT_VENDOR_BILL", "Edit Vendor Bill", "VENDOR_BILL"},
                new Object[]{"APPROVE_VENDOR_BILL", "Approve Vendor Bill", "VENDOR_BILL"},
                new Object[]{"POST_VENDOR_BILL", "Post Vendor Bill", "VENDOR_BILL"},
                new Object[]{"CANCEL_VENDOR_BILL", "Cancel Vendor Bill", "VENDOR_BILL"},

                new Object[]{"VIEW_PAYMENT", "View Payment", "PAYMENT"},
                new Object[]{"CREATE_PAYMENT", "Create Payment", "PAYMENT"},
                new Object[]{"POST_PAYMENT", "Post Payment", "PAYMENT"},
                new Object[]{"CANCEL_PAYMENT", "Cancel Payment", "PAYMENT"},

                new Object[]{"VIEW_LEDGER", "View Ledger", "REPORT"},
                new Object[]{"VIEW_TRIAL_BALANCE", "View Trial Balance", "REPORT"},
                new Object[]{"VIEW_REPORT", "View Reports", "REPORT"},

                new Object[]{"VIEW_BANKING", "View Banking", "BANKING"},
                new Object[]{"CREATE_BANKING", "Create Banking", "BANKING"},
                new Object[]{"EDIT_BANKING", "Edit Banking", "BANKING"},

                new Object[]{"MANAGE_USERS", "Manage Users", "USER_MANAGEMENT"},
                new Object[]{"MANAGE_ROLES", "Manage Roles", "USER_MANAGEMENT"},
                new Object[]{"MANAGE_PERMISSIONS", "Manage Permissions", "USER_MANAGEMENT"},

                new Object[]{"VIEW_AUDIT_LOGS", "View Audit Logs", "AUDIT"},

                new Object[]{"MANAGE_SETTINGS", "Manage System Settings", "SETTINGS"}
        );

        for (Object[] p : permissions) {
            String code = (String) p[0];
            // Skip if  exists
            if (!permissionRepository.existsByCode(code)) {
                permissionRepository.save(Permission.builder()
                        .code(code)
                        .name((String) p[1])
                        .module((String) p[2])
                        .build());
            }
        }
    }


    // -----------Appoint Default Roles with permission

    private void seedRoles() {

        // SUPER_ADMIN -- all permissions
        createRoleIfNotExists("SUPER_ADMIN", "Super Administrator",
                permissionRepository.findAll());

        // ACCOUNTANT -- all except user management
        createRoleIfNotExists("ACCOUNTANT", "Accountant",
                permissionRepository.findAll().stream()
                        .filter(p -> !p.getModule().equals("USER_MANAGEMENT"))
                        .toList());

        // SALES_MANAGER
        createRoleIfNotExists("SALES_MANAGER", "Sales Manager",
                permissionRepository.findByModule("PARTY").stream()
                        .filter(p -> List.of("VIEW_PARTY", "CREATE_PARTY", "EDIT_PARTY").contains(p.getCode()))
                        .toList());

        // PURCHASE_MANAGER
        createRoleIfNotExists("PURCHASE_MANAGER", "Purchase Manager",
                permissionRepository.findByModule("VENDOR_BILL"));

        // VIEWER -- all VIEW_ permissions only, excluding audit trail (sensitive, admin/accountant only)
        createRoleIfNotExists("VIEWER", "Viewer",
                permissionRepository.findAll().stream()
                        .filter(p -> p.getCode().startsWith("VIEW_") && !p.getModule().equals("AUDIT"))
                        .toList());
    }

    private void createRoleIfNotExists(String name, String description,
                                       List<Permission> permissions) {
        if (!roleRepository.existsByName(name)) {
            roleRepository.save(Role.builder()
                    .name(name)
                    .description(description)
                    .permissions(new HashSet<>(permissions))
                    .build());
        }
    }


    //--- ASSIGN DEFAULT SUPER_ADMIN USER
    private void seedAdminUser() {

        if (!userRepository.existsByEmail(adminEmail)) {

            Role superAdminRole = roleRepository.findByName("SUPER_ADMIN")
                    .orElseThrow(() -> new RuntimeException("SUPER_ADMIN role not found"));

            Set<Role> roles = new HashSet<>();
            roles.add(superAdminRole);

            userRepository.save(User.builder()
                    .name("Super Admin")
                    .email(adminEmail)
                    .password(passwordEncoder.encode(adminPassword))
                    .status(UserStatus.ACTIVE)
                    .failedLoginAttempts(0)
                    .roles(roles)
                    .build());
        }
    }

}