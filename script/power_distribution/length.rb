module Length
  MICRONS_PER_IN = 25_400

  def Length.microns_to_ft(microns)
    inches = microns / MICRONS_PER_IN
    inches / 12
  end

  def Length.microns_to_ft_s(microns)
    ft = microns_to_ft(microns)
    in_remain = ft.modulo(1) * 12
    "#{ft.to_i}' #{in_remain.round(1)}\""
  end
end