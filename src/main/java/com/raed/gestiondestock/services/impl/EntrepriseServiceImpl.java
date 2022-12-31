package com.raed.gestiondestock.services.impl;

import com.raed.gestiondestock.dto.EntrepriseDto;
import com.raed.gestiondestock.dto.RolesDto;
import com.raed.gestiondestock.dto.UtilisateurDto;
import com.raed.gestiondestock.exception.EntityNotFoundException;
import com.raed.gestiondestock.exception.ErrorCodes;
import com.raed.gestiondestock.exception.InvalidEntityException;
import com.raed.gestiondestock.repository.EntrepriseRepository;
import com.raed.gestiondestock.repository.RolesRepository;
import com.raed.gestiondestock.services.EntrepriseService;
import com.raed.gestiondestock.services.UtilisateurService;
import com.raed.gestiondestock.validator.EntrepriseValidator;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;
import javax.transaction.Transactional;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Transactional(rollbackOn = Exception.class)
@Service
@Slf4j
public class EntrepriseServiceImpl implements EntrepriseService {

  private EntrepriseRepository entrepriseRepository;
  private UtilisateurService utilisateurService;
  private RolesRepository rolesRepository;

  @Autowired
  public EntrepriseServiceImpl(EntrepriseRepository entrepriseRepository, UtilisateurService utilisateurService,
      RolesRepository rolesRepository) {
    this.entrepriseRepository = entrepriseRepository;
    this.utilisateurService = utilisateurService;
    this.rolesRepository = rolesRepository;
  }

  @Override
  public EntrepriseDto save(EntrepriseDto dto) {
    List<String> errors = EntrepriseValidator.validate(dto);
    if (!errors.isEmpty()) {
      log.error("Entreprise is not valid {}", dto);
      throw new InvalidEntityException("L'entreprise n'est pas valide", ErrorCodes.ENTREPRISE_NOT_VALID, errors);
    }
    EntrepriseDto savedEntreprise = EntrepriseDto.fromEntity(
        entrepriseRepository.save(EntrepriseDto.toEntity(dto))
    );

    UtilisateurDto utilisateur = fromEntreprise(savedEntreprise);

    UtilisateurDto savedUser = utilisateurService.save(utilisateur);

    RolesDto rolesDto = RolesDto.builder()
        .roleName("ADMIN")
        .utilisateur(savedUser)
        .build();

    rolesRepository.save(RolesDto.toEntity(rolesDto));

    return  savedEntreprise;
  }

  private UtilisateurDto fromEntreprise(EntrepriseDto dto) {
    return UtilisateurDto.builder()
        .adresse(dto.getAdresse())
        .nom(dto.getNom())
        .prenom(dto.getCodeFiscal())
        .email(dto.getEmail())
        .moteDePasse(generateRandomPassword())
        .entreprise(dto)
        .dateDeNaissance(Instant.now())
        .photo(dto.getPhoto())
        .build();
  }

  private String generateRandomPassword() {
    return "som3R@nd0mP@$$word";
  }

  @Override
  public EntrepriseDto findById(Integer id) {
    if (id == null) {
      log.error("Entreprise ID is null");
      return null;
    }
    return entrepriseRepository.findById(id)
        .map(EntrepriseDto::fromEntity)
        .orElseThrow(() -> new EntityNotFoundException(
            "Aucune entreprise avec l'ID = " + id + " n' ete trouve dans la BDD",
            ErrorCodes.ENTREPRISE_NOT_FOUND)
        );
  }

  @Override
  public List<EntrepriseDto> findAll() {
    return entrepriseRepository.findAll().stream()
        .map(EntrepriseDto::fromEntity)
        .collect(Collectors.toList());
  }

  @Override
  public void delete(Integer id) {
    if (id == null) {
      log.error("Entreprise ID is null");
      return;
    }
    entrepriseRepository.deleteById(id);
  }
}
