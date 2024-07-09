/*
 * Copyright 2016-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.samples.petclinic.rest.controller;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.samples.petclinic.mapper.PetMapper;
import org.springframework.samples.petclinic.mapper.VisitMapper;
import org.springframework.samples.petclinic.model.Pet;
import org.springframework.samples.petclinic.rest.api.PetsApi;
import org.springframework.samples.petclinic.rest.dto.PetDto;
import org.springframework.samples.petclinic.rest.dto.VisitDto;
import org.springframework.samples.petclinic.service.ClinicService;
import org.springframework.samples.petclinic.service.DiseaseRiskAi;
import org.springframework.samples.petclinic.service.PedigreeService;
import org.springframework.samples.petclinic.service.PetRegistryService;
import org.springframework.samples.petclinic.service.VetControlService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Vitaliy Fedoriv
 */

@RestController
@CrossOrigin(exposedHeaders = "errors, content-type")
@RequestMapping("api")
public class PetRestController implements PetsApi {

    private final ClinicService clinicService;

    private final PetMapper petMapper;

    private final VisitMapper visitMapper;

    @Nullable
    private final DiseaseRiskAi diseaseRiskAi;

    @Nullable
    private final PedigreeService pedigreeService;

    @Nullable
    private final PetRegistryService petRegistryService;

    @Nullable
    private final VetControlService vetControlService;

    public PetRestController(ClinicService clinicService,
                             PetMapper petMapper,
                             VisitMapper visitMapper,
                             @Nullable DiseaseRiskAi diseaseRiskAi,
                             @Nullable PedigreeService pedigreeService,
                             @Nullable PetRegistryService petRegistryService,
                             @Nullable VetControlService vetControlService) {
        this.clinicService = clinicService;
        this.petMapper = petMapper;
        this.visitMapper = visitMapper;
        this.diseaseRiskAi = diseaseRiskAi;
        this.pedigreeService = pedigreeService;
        this.petRegistryService = petRegistryService;
        this.vetControlService = vetControlService;
    }

    @PreAuthorize("hasRole(@roles.OWNER_ADMIN)")
    @Override
    public ResponseEntity<PetDto> getPet(Integer petId) {
        Pet petById = this.clinicService.findPetById(petId);
        PetDto pet = petMapper.toPetDto(petById);
        if (pet == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        if (vetControlService != null && List.of("snake", "lizard").contains(petById.getType().getName())) {
            vetControlService.notifyOnDangerousAnimal(petById);
        }
        return new ResponseEntity<>(pet, HttpStatus.OK);
    }

    @PreAuthorize("hasRole(@roles.OWNER_ADMIN)")
    @Override
    public ResponseEntity<List<PetDto>> listPets() {
        Collection<Pet> allPets = (petRegistryService == null)      // the service may be disabled
            ? clinicService.findAllPets()
            : clinicService.findAllPets().stream()
                           .filter(petRegistryService::isPetRegistered)
                           .toList();
        List<PetDto> pets = new ArrayList<>(petMapper.toPetsDto(allPets));
        if (pets.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        return new ResponseEntity<>(pets, HttpStatus.OK);
    }

	@PreAuthorize("hasRole(@roles.OWNER_ADMIN)")
    @Override
	public ResponseEntity<List<VisitDto>> listRecommendedVisits(Integer petId) {
        if (diseaseRiskAi == null) {        // the service may be disabled
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
        }
        List<VisitDto> visits = new ArrayList<>(visitMapper.toVisitsDto(diseaseRiskAi.fetchRecommendedVisits(petId)));
        if (visits.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(visits);
	}

	@PreAuthorize("hasRole(@roles.OWNER_ADMIN)")
    @Override
    public ResponseEntity<PetDto> updatePet(Integer petId, PetDto petDto) {
        Pet currentPet = this.clinicService.findPetById(petId);
        if (currentPet == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        // pets belonging to pedigree beta testing must be processed separately
        if (pedigreeService != null && pedigreeService.isPetInBetaTesting(currentPet.getName())) {
            pedigreeService.updatePedigreeByRequest(currentPet.getName());
            return new ResponseEntity<>(petMapper.toPetDto(currentPet), HttpStatus.I_AM_A_TEAPOT);
        }
        else {
            currentPet.setBirthDate(petDto.getBirthDate());
            currentPet.setName(petDto.getName());
            currentPet.setType(petMapper.toPetType(petDto.getType()));
            this.clinicService.savePet(currentPet);
            return new ResponseEntity<>(petMapper.toPetDto(currentPet), HttpStatus.NO_CONTENT);
        }
    }

    @PreAuthorize("hasRole(@roles.OWNER_ADMIN)")
    @Override
    public ResponseEntity<PetDto> deletePet(Integer petId) {
        Pet pet = this.clinicService.findPetById(petId);
        if (pet == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        this.clinicService.deletePet(pet);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @PreAuthorize("hasRole(@roles.OWNER_ADMIN)")
    @Override
    public ResponseEntity<PetDto> addPet(PetDto petDto) {
        this.clinicService.savePet(petMapper.toPet(petDto));
        return new ResponseEntity<>(petDto, HttpStatus.OK);
    }
}
