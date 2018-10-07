package com.ecommerce.microcommerce.web.controller;

import com.ecommerce.microcommerce.dao.ProductDao;
import com.ecommerce.microcommerce.model.Product;
import com.ecommerce.microcommerce.web.exceptions.ProduitGratuitException;
import com.ecommerce.microcommerce.web.exceptions.ProduitIntrouvableException;
import com.fasterxml.jackson.databind.ser.FilterProvider;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.json.MappingJacksonValue;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import javax.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;


@Api( description="API pour les opérations CRUD sur les produits.")
@RestController
public class ProductController {

  @Autowired
  private ProductDao productDao;

  private static SimpleBeanPropertyFilter monFiltre = SimpleBeanPropertyFilter.serializeAllExcept("prixAchat");

  private static FilterProvider listDeNosFiltres = new SimpleFilterProvider().addFilter("monFiltreDynamique", monFiltre);


  //Récupérer la liste des produits
  @RequestMapping(value = "/Produits", method = RequestMethod.GET)
  public MappingJacksonValue listeProduits() {
    Iterable<Product> produits = productDao.findAll();

    MappingJacksonValue produitsFiltres = new MappingJacksonValue(produits);
    produitsFiltres.setFilters(listDeNosFiltres);

    return produitsFiltres;
  }


  //Récupérer un produit par son Id
  @ApiOperation(value = "Récupère un produit grâce à son ID à condition que celui-ci soit en stock!")
  @GetMapping(value = "/Produits/{id}")
  public MappingJacksonValue afficherUnProduit(@PathVariable int id) {
    Product produit = productDao.findById(id);

    if(produit==null)
      throw new ProduitIntrouvableException("Le produit avec l'id " + id + " est INTROUVABLE. Écran Bleu si je pouvais.");

    MappingJacksonValue filteredProduct = new MappingJacksonValue(produit);
    filteredProduct.setFilters(listDeNosFiltres);

    return filteredProduct;
  }

  //ajouter un produit
  @ApiOperation(value = "Créer un nouveau produit")
  @PostMapping(value = "/Produits")
  public ResponseEntity<Void> ajouterProduit(@Valid @RequestBody Product product) {

    if (product.getPrix() == 0)
      throw new ProduitGratuitException("Le produit ne peut pas être gratuit");

    Product productAdded =  productDao.save(product);

    if (productAdded == null)
      return ResponseEntity.noContent().build();

    URI location = ServletUriComponentsBuilder
        .fromCurrentRequest()
        .path("/{id}")
        .buildAndExpand(productAdded.getId())
        .toUri();

    return ResponseEntity.created(location).build();
  }


  @ApiOperation(value = "Supprimer un produit")
  @DeleteMapping (value = "/Produits/{id}")
  public void supprimerProduit(@PathVariable int id) {
    productDao.delete(id);
  }


  @ApiOperation(value = "Mettre à jour un produit existant")
  @PutMapping (value = "/Produits")
  public void updateProduit(@RequestBody Product product) {

    if (product.getPrix() == 0)
      throw new ProduitGratuitException("Le produit ne peut pas être gratuit");

    productDao.save(product);
  }


  //Pour les tests
  @GetMapping(value = "test/produits/{prix}")
  public List<Product>  testeDeRequetes(@PathVariable int prix) {
    return productDao.chercherUnProduitCher(400);
  }


  // Marge de Chq Produit
  @ApiOperation(value = "Calcule la marge de chaque produit")
  @GetMapping(value = "/AdminProduits")
  public List<Object> calculMargeProduits() {
    return productDao.findAll()
        .stream()
        .flatMap(product -> Stream.of(product, product.getPrix() - product.getPrixAchat()))
        .collect(Collectors.toList());
  }

  // Ordre Croissant
  @ApiOperation(value = "Retourne la liste des produits triés par nom croissant")
  @GetMapping(value = "/ProduitsOrdre")
  public MappingJacksonValue trierProduitsParOrdreAlphabetique() {

    Iterable<Product> products = productDao.findAllByOrderByNomAsc();
    FilterProvider filter = new SimpleFilterProvider().addFilter("monFiltreDynamique", SimpleBeanPropertyFilter.serializeAllExcept("prixAchat"));

    MappingJacksonValue productFiltered = new MappingJacksonValue(products);
    productFiltered.setFilters(filter);

    return productFiltered;
  }
}
