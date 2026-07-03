package com.nexaerp.settings;


import com.nexaerp.account.Account;
import com.nexaerp.account.AccountRepository;
import com.nexaerp.common.exception.ResourceNotFoundException;
import org.springframework.cache.annotation.Cacheable;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SystemSettingsServiceImpl implements SystemSettingsService{

    private final SystemSettingRepository systemSettingRepository;
    private final AccountRepository accountRepository;


    @Override
    @Cacheable(value = "systemSettings", key = "#key")
    public Account getAccount(SettingKey key) {
        Long accountId = getAccountId(key);
        return accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Account not found for setting: " + key));
    }

    @Override
    @Cacheable(value = "systemSettings", key = "#key + '_id'")
    public Long getAccountId(SettingKey key) {
        String value = getValue(key);
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            throw new ResourceNotFoundException(
                    "Invalid account ID in settings for key: " + key);
        }
    }

    @Override
    public String getValue(SettingKey key) {
        return systemSettingRepository.findByKey(key)
                .map(SystemSetting::getValue)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Setting not found: " + key));
    }

    @Override
    @CacheEvict(value = "systemSettings", allEntries = true)
    public void updateSetting(SettingKey key, String value) {
        SystemSetting setting = systemSettingRepository.findByKey(key)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Setting not found: " + key));
        setting.setValue(value);
        systemSettingRepository.save(setting);

    }
}
