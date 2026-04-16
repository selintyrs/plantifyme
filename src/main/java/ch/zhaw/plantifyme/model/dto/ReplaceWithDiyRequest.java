package ch.zhaw.plantifyme.model.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class ReplaceWithDiyRequest {
    private String diyRecipeId;
    private List<Integer> selectedIndexes;
    private String originalDisplayName;
}
