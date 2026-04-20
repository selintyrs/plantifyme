package ch.zhaw.plantifyme.service;

import ch.zhaw.plantifyme.model.DiyRecipe;
import ch.zhaw.plantifyme.model.DiyRecipeDto;
import ch.zhaw.plantifyme.repository.DiyRecipeRepository;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DiyRecipeService {

    @NonNull
    private final DiyRecipeRepository diyRecipeRepository;

    public List<DiyRecipeDto> findAll() {
        return diyRecipeRepository.findAll()
                .stream()
                .map(this::toDto)
                .toList();
    }

    public List<DiyRecipeDto> findByMatching(String displayName, String subcategory) {
        String sub = (subcategory == null || subcategory.isBlank()) ? "" : subcategory;
        return diyRecipeRepository.findByDisplayNameAndSubcategory(displayName, sub)
                .stream()
                .map(this::toDto)
                .toList();
    }

    public DiyRecipeDto findById(String id) {
        return diyRecipeRepository.findById(id)
                .map(this::toDto)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "DIY recipe not found: " + id));
    }

    private DiyRecipeDto toDto(DiyRecipe recipe) {
        DiyRecipeDto dto = new DiyRecipeDto();
        dto.setId(recipe.getId());
        dto.setTitle(recipe.getTitle());
        dto.setDescription(recipe.getDescription());
        dto.setPrepTimeMin(recipe.getPrepTimeMin());
        dto.setDifficulty(recipe.getDifficulty());
        dto.setImageUrl(recipe.getImageUrl());
        dto.setUseCase(recipe.getUseCase());
        dto.setMatchingKeys(recipe.getMatchingKeys());
        dto.setIngredients(recipe.getIngredients());
        dto.setInstructions(recipe.getInstructions());
        return dto;
    }
}
