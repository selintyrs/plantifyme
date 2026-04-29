package ch.zhaw.plantifyme.service;

import ch.zhaw.plantifyme.model.BaseRecipe;
import ch.zhaw.plantifyme.model.dto.BaseRecipeDto;
import ch.zhaw.plantifyme.model.dto.BaseRecipeListItemDto;
import ch.zhaw.plantifyme.repository.BaseRecipeRepository;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BaseRecipeService {

    @NonNull
    private final BaseRecipeRepository baseRecipeRepository;

    public List<BaseRecipeDto> findAll() {
        return baseRecipeRepository.findAll().stream()
                .map(this::toDto)
                .toList();
    }

    public List<BaseRecipeListItemDto> findAllSlim() {
        return baseRecipeRepository.findAll().stream()
                .map(r -> new BaseRecipeListItemDto(
                        r.getId(),
                        r.getTitle(),
                        r.getCategory(),
                        r.getImageUrl(),
                        r.getPrepTimeMin(),
                        r.getDifficulty()
                ))
                .toList();
    }

    public List<BaseRecipeDto> findByCategory(String category) {
        return baseRecipeRepository.findByCategory(category).stream()
                .map(this::toDto)
                .toList();
    }

    public BaseRecipeDto findById(String id) {
        return baseRecipeRepository.findById(id)
                .map(this::toDto)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Base recipe not found: " + id));
    }

    private BaseRecipeDto toDto(BaseRecipe recipe) {
        BaseRecipeDto dto = new BaseRecipeDto();
        dto.setId(recipe.getId());
        dto.setTitle(recipe.getTitle());
        dto.setDescription(recipe.getDescription());
        dto.setCategory(recipe.getCategory());
        dto.setPrepTimeMin(recipe.getPrepTimeMin());
        dto.setDifficulty(recipe.getDifficulty());
        dto.setDefaultServings(recipe.getDefaultServings());
        dto.setImageUrl(recipe.getImageUrl());
        dto.setIngredients(recipe.getIngredients());
        dto.setInstructions(recipe.getInstructions());
        dto.setTotalNutritionPerServing(recipe.getTotalNutritionPerServing());
        return dto;
    }
}
