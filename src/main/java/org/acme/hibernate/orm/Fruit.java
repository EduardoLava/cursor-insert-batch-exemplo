package org.acme.hibernate.orm;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

@Entity
@Table(name = "fruit")
public class Fruit {

	@Id
  @GeneratedValue(
      strategy = GenerationType.SEQUENCE, 
      generator = "fruit_seq"
  )
  @SequenceGenerator(
      name = "fruit_seq", 
      sequenceName = "fruit_seq", 
      allocationSize = 100
  )
	private Integer id;

	@Column(length = 40)
	private String name;

	public Fruit() {
	}

	public Fruit(String name) {
		this.name = name;
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

}
