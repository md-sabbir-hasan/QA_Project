package com.nexaerp.account;

import com.nexaerp.account.dto.AccountRequestDto;
import com.nexaerp.account.dto.AccountResponseDto;
import com.nexaerp.common.exception.BusinessRuleException;
import com.nexaerp.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService{

    private final AccountRepository accountRepository;

    @Override
    public AccountResponseDto create(AccountRequestDto request) {
        // Code unique check
        if (accountRepository.existsByCode(request.getCode())) {
            throw new BusinessRuleException("Account code already exists: " + request.getCode());
        }

        Account account = new Account();
        account.setCode(request.getCode());
        account.setName(request.getName());
        account.setDescription(request.getDescription());
        account.setType(request.getType());
        account.setIsActive(true);
        account.setIsDefault(false);

        // Parent set
        if (request.getParentId() != null) {
            Account parent = accountRepository.findById(request.getParentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Parent account not found"));

            // Child type = parent type
            if (!parent.getType().equals(request.getType())) {
                throw new BusinessRuleException("Child account type must match parent account type");
            }

            account.setParent(parent);
        }

        Account saved = accountRepository.save(account);
        return toResponse(saved);
    }

    @Override
    public AccountResponseDto update(Long id, AccountRequestDto request) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));

        //Can not change Default account type
        if (account.getIsDefault() && !account.getType().equals(request.getType())) {
            throw new BusinessRuleException("Cannot change type of a default account");
        }

        account.setName(request.getName());
        account.setDescription(request.getDescription());

        Account saved = accountRepository.save(account);
        return toResponse(saved);
    }

    @Override
    public AccountResponseDto getById(Long id) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));
        return toResponse(account);
    }

    @Override
    public List<AccountResponseDto> getAll() {
        return accountRepository.findAll()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<AccountResponseDto> getTree() {
        // only take root accounts, children comes auto recursively
        return accountRepository.findByParentIsNull()
                .stream()
                .map(this::toTreeResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<AccountResponseDto> getByType(AccountType type) {

        return accountRepository.findByType(type)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public void deactivate(Long id) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));

        if (account.getIsDefault()) {
            throw new BusinessRuleException("Cannot deactivate a default system account");
        }

        account.setIsActive(false);
        accountRepository.save(account);

    }

                                  // -- Mapper --

    private AccountResponseDto toResponse(Account account) {
        AccountResponseDto dto = new AccountResponseDto();
        dto.setId(account.getId());
        dto.setCode(account.getCode());
        dto.setName(account.getName());
        dto.setDescription(account.getDescription());
        dto.setType(account.getType());
        dto.setIsActive(account.getIsActive());
        dto.setIsDefault(account.getIsDefault());
        dto.setCurrentBalance(account.getCurrentBalance());

        if (account.getParent() != null) {
            dto.setParentId(account.getParent().getId());
            dto.setParentName(account.getParent().getName());
        }

        return dto;
    }

    private AccountResponseDto toTreeResponse(Account account) {
        AccountResponseDto dto = toResponse(account);

        if (account.getChildren() != null && !account.getChildren().isEmpty()) {
            dto.setChildren(
                    account.getChildren()
                            .stream()
                            .map(this::toTreeResponse) // Recursive
                            .collect(Collectors.toList())
            );
        }

        return dto;
    }
}
