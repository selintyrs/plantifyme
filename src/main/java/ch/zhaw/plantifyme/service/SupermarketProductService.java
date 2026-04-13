package ch.zhaw.plantifyme.service;

import ch.zhaw.plantifyme.model.SupermarketProduct;
import ch.zhaw.plantifyme.repository.SupermarketProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

@Service
public class SupermarketProductService {

    @Autowired
    private SupermarketProductRepository supermarketProductRepository;

    public Optional<List<SupermarketProduct>> getAlternatives(String productId, String filter) {
        Optional<SupermarketProduct> original = supermarketProductRepository.findById(productId);
        if (original.isEmpty()) {
            return Optional.empty();
        }

        SupermarketProduct product = original.get();
        List<SupermarketProduct> alternatives = supermarketProductRepository
                .findByPlantifyCatL4AndSupermarketAndIdNot(
                        product.getPlantifyCatL4(),
                        product.getSupermarket(),
                        productId
                );

        Predicate<SupermarketProduct> filterPredicate;
        Comparator<SupermarketProduct> comparator;

        if (filter == null || filter.isBlank() || filter.equals("default")) {
            filterPredicate = p -> true;
            comparator = Comparator.comparing(SupermarketProduct::getPrice, Comparator.nullsLast(Comparator.naturalOrder()));
        } else switch (filter) {
            case "cheaper" -> {
                filterPredicate = p -> p.getPrice() != null
                        && product.getPrice() != null
                        && p.getPrice() < product.getPrice();
                comparator = Comparator.comparing(SupermarketProduct::getPrice, Comparator.nullsLast(Comparator.naturalOrder()));
            }
            case "healthier" -> {
                filterPredicate = p -> p.getProteinG() != null
                        && product.getProteinG() != null
                        && p.getProteinG() > product.getProteinG();
                comparator = Comparator.comparing(SupermarketProduct::getProteinG, Comparator.nullsLast(Comparator.reverseOrder()));
            }
            case "lowcal" -> {
                filterPredicate = p -> p.getEnergyKcal() != null
                        && product.getEnergyKcal() != null
                        && p.getEnergyKcal() < product.getEnergyKcal();
                comparator = Comparator.comparing(SupermarketProduct::getEnergyKcal, Comparator.nullsLast(Comparator.naturalOrder()));
            }
            default -> {
                filterPredicate = p -> true;
                comparator = Comparator.comparing(SupermarketProduct::getPrice, Comparator.nullsLast(Comparator.naturalOrder()));
            }
        }

        List<SupermarketProduct> result = alternatives.stream()
                .filter(filterPredicate)
                .sorted(comparator)
                .limit(4)
                .toList();

        

        return Optional.of(result);
    }
}
