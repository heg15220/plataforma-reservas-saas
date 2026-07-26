export function reservationList() {
  return {
    items: [
      {
        id: "10000000-0000-4000-8000-000000000001",
        timeSlotId: "20000000-0000-4000-8000-000000000001",
        customerName: "Ana Martín",
        customerEmail: "ana@example.com",
        partySize: 2,
        date: "2026-07-26",
        startsAt: "10:00:00",
        endsAt: "11:00:00",
        status: "confirmed",
        createdAt: "2026-07-25T09:00:00Z",
      },
    ],
    page: 0,
    size: 100,
    totalElements: 1,
    totalPages: 1,
  };
}

export function reservationDetail() {
  return {
    ...reservationList().items[0],
    serviceId: "30000000-0000-4000-8000-000000000001",
    cancelledAt: null,
    cancelledBy: null,
    cancellationReason: null,
    updatedAt: "2026-07-25T09:01:00Z",
    formAnswers: [
      {
        fieldKey: "allergies",
        fieldLabel: "Alergias",
        value: "Ninguna",
        createdAt: "2026-07-25T09:00:30Z",
      },
    ],
    assignedResource: {
      id: "40000000-0000-4000-8000-000000000001",
      type: "professional",
      firstName: "Lucía",
      lastName: "Martín",
      publicAlias: "Lucía",
      specialty: "Estilismo",
      status: "active",
    },
    incidentHistory: {
      totalElements: 1,
      truncated: false,
      items: [
        {
          incidentType: "late_cancellation",
          reportedAt: "2026-06-01T12:00:00Z",
          status: "reported",
        },
      ],
    },
  };
}
