package com.example.isp392.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "publishers")
public class Publisher {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "publisher_id")
    private Integer publisherId;

    @Column(name = "publisher_name", length = 200) // Example field
    private String publisherName;

    // A publisher can publish many books
    @OneToMany(mappedBy = "publisher", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Book> books = new ArrayList<>();;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "contact_info", length = 500)
    private String contactInfo;

}
